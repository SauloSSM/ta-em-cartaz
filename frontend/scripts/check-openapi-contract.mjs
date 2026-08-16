import { readFileSync } from 'node:fs';
import ts from 'typescript';

const contract = readFileSync(
  new URL('../../openapi/elite-dev-ticket-v1.yaml', import.meta.url),
  'utf8',
).replaceAll('\r\n', '\n');

const authTypeSource = readFileSync(new URL('../src/app/api/authApi.ts', import.meta.url), 'utf8');
const catalogTypeSource = readFileSync(
  new URL('../src/features/catalog/api/catalogApi.ts', import.meta.url),
  'utf8',
);
const eventsTypeSource = readFileSync(
  new URL('../src/features/events/api/eventsApi.ts', import.meta.url),
  'utf8',
);
const reservationsTypeSource = readFileSync(
  new URL('../src/features/reservations/api/reservationsApi.ts', import.meta.url),
  'utf8',
);
const paymentsTypeSource = readFileSync(
  new URL('../src/features/payments/api/paymentsApi.ts', import.meta.url),
  'utf8',
);
const ticketsTypeSource = readFileSync(
  new URL('../src/features/tickets/api/ticketsApi.ts', import.meta.url),
  'utf8',
);

const authSourceFile = ts.createSourceFile(
  'authApi.ts',
  authTypeSource,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS,
);
const catalogSourceFile = ts.createSourceFile(
  'catalogApi.ts',
  catalogTypeSource,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS,
);
const eventsSourceFile = ts.createSourceFile(
  'eventsApi.ts',
  eventsTypeSource,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS,
);
const reservationsSourceFile = ts.createSourceFile(
  'reservationsApi.ts',
  reservationsTypeSource,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS,
);
const paymentsSourceFile = ts.createSourceFile(
  'paymentsApi.ts',
  paymentsTypeSource,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS,
);
const ticketsSourceFile = ts.createSourceFile(
  'ticketsApi.ts',
  ticketsTypeSource,
  ts.ScriptTarget.Latest,
  true,
  ts.ScriptKind.TS,
);

const allStatements = [
  ...authSourceFile.statements,
  ...catalogSourceFile.statements,
  ...eventsSourceFile.statements,
  ...reservationsSourceFile.statements,
  ...paymentsSourceFile.statements,
  ...ticketsSourceFile.statements,
];

const aliases = new Map(
  allStatements
    .filter(ts.isTypeAliasDeclaration)
    .map((declaration) => [declaration.name.text, declaration]),
);

const clientFunctions = new Map(
  allStatements
    .filter(ts.isFunctionDeclaration)
    .filter((declaration) => declaration.name !== undefined)
    .map((declaration) => [declaration.name.text, declaration]),
);

const operations = parseOperations(contract);
const securitySchemes = parseSecuritySchemes(contract);
const componentResponses = parseComponentResponses(contract);
const schemaBlocks = new Map(parseSchemas(contract).map((schema) => [schema.name, schema.block]));
const schemas = new Map();

for (const operation of operations) {
  assertClientOperation(operation, clientFunctions, componentResponses);
}
assertSecuritySemantics(operations, securitySchemes);

for (const schemaName of reachableSchemas(operations, componentResponses, schemaBlocks, schemas)) {
  const schema = schemas.get(schemaName);
  if (schema === undefined) {
    fail(`a API referencia o schema inexistente ${schemaName}`);
  }
  const alias = aliases.get(schema.name);
  if (alias === undefined) {
    fail(`o schema ${schema.name} não possui tipo TypeScript correspondente`);
  }

  if (schema.kind === 'object') {
    assertObjectAlias(schema, alias);
  } else if (schema.kind === 'enum') {
    assertUnionAlias(schema.name, schema.values, alias);
  } else {
    assertUnionAlias(schema.name, schema.members, alias);
  }
}

function parseOperations(source) {
  const start = source.indexOf('paths:\n');
  const end = source.indexOf('\ncomponents:', start);
  if (start < 0 || end < 0) {
    fail('paths não foi encontrado no OpenAPI');
  }

  const section = source.slice(start + 'paths:\n'.length, end);
  const pathMarkers = [...section.matchAll(/^  (\/[^:]+):$/gm)];
  return pathMarkers.flatMap((pathMarker, index) => {
    const pathStart = pathMarker.index + pathMarker[0].length + 1;
    const pathEnd = pathMarkers[index + 1]?.index ?? section.length;
    const pathBlock = section.slice(pathStart, pathEnd);
    if (!pathMarker[1].startsWith('/api/v1/auth/')
      && !pathMarker[1].startsWith('/api/v1/catalog/')
      && !pathMarker[1].startsWith('/api/v1/events')
      && !pathMarker[1].startsWith('/api/v1/reservations')
      && !pathMarker[1].startsWith('/api/v1/my-tickets')) {
      return [];
    }
    const methodMarkers = [...pathBlock.matchAll(/^    (get|post|put|patch|delete|options|head|trace):$/gm)];
    if (methodMarkers.length === 0) {
      fail(`operação sem método suportado em ${pathMarker[1]}`);
    }
    return methodMarkers.map((methodMarker, methodIndex) => {
      const methodStart = methodMarker.index + methodMarker[0].length + 1;
      const methodEnd = methodMarkers[methodIndex + 1]?.index ?? pathBlock.length;
      const methodBlock = pathBlock.slice(methodStart, methodEnd);
      const operationLabel = `${methodMarker[1].toUpperCase()} ${pathMarker[1]}`;
      const operationId = methodBlock.match(/^      operationId: ([A-Za-z][A-Za-z0-9]*)$/m)?.[1];
      if (operationId === undefined) {
        fail(`operationId ausente em ${operationLabel}`);
      }

      const requestBlock = indentedSection(methodBlock, 'requestBody', 6);
      const requestSchema = requestBlock === undefined
        ? undefined
        : requestBlock.match(/^\s+\$ref: '#\/components\/schemas\/([^']+)'$/m)?.[1];
      if (requestBlock !== undefined
        && (!requestBlock.includes('        required: true\n') || requestSchema === undefined)) {
        fail(`requestBody inválido em ${operationLabel}`);
      }

      return {
        path: pathMarker[1],
        method: methodMarker[1].toUpperCase(),
        operationId,
        requestSchema,
        security: parseSecurity(indentedSection(methodBlock, 'security', 6), operationLabel),
        responses: parseOperationResponses(indentedSection(methodBlock, 'responses', 6), operationLabel),
      };
    });
  });
}

function reachableSchemas(operations, responses, schemaBlocks, schemas) {
  const pending = [];
  for (const operation of operations) {
    if (operation.requestSchema !== undefined) pending.push(operation.requestSchema);
    for (const response of operation.responses.values()) {
      if (response.schemaRef !== undefined) pending.push(response.schemaRef);
      if (response.componentRef !== undefined) {
        const component = responses.get(response.componentRef);
        if (component === undefined) {
          fail(`a operação referencia a response inexistente ${response.componentRef}`);
        }
        if (component.schemaRef !== undefined) pending.push(component.schemaRef);
      }
    }
  }

  const reachable = new Set();
  while (pending.length > 0) {
    const name = pending.pop();
    if (reachable.has(name)) continue;
    reachable.add(name);
    const block = schemaBlocks.get(name);
    if (block === undefined) continue;
    const schema = parseSchema(name, block);
    schemas.set(name, schema);
    if (schema.kind === 'union') {
      pending.push(...schema.members);
    } else if (schema.kind === 'object') {
      for (const property of schema.properties) {
        const reference = property.type.endsWith('[]') ? property.type.slice(0, -2) : property.type;
        if (schemaBlocks.has(reference)) pending.push(reference);
      }
    }
  }
  return reachable;
}

function parseSecurity(block, path) {
  if (block === undefined) {
    fail(`security ausente em ${path}`);
  }
  const requirements = [];
  let current;
  for (const line of block.split('\n')) {
    const first = line.match(/^        - ([A-Za-z][A-Za-z0-9]*): \[\]$/)?.[1];
    if (first !== undefined) {
      current = new Set([first]);
      requirements.push(current);
      continue;
    }
    if (line === '        - {}') {
      current = new Set();
      requirements.push(current);
      continue;
    }
    const additional = line.match(/^          ([A-Za-z][A-Za-z0-9]*): \[\]$/)?.[1];
    if (additional !== undefined && current !== undefined) {
      current.add(additional);
    }
  }
  if (requirements.length === 0) {
    fail(`security vazio ou não suportado em ${path}`);
  }
  return requirements;
}

function parseOperationResponses(block, path) {
  if (block === undefined) {
    fail(`responses ausente em ${path}`);
  }
  const markers = [...block.matchAll(/^        '(\d{3})':$/gm)];
  if (markers.length === 0) {
    fail(`responses vazio em ${path}`);
  }
  return new Map(markers.map((marker, index) => {
    const responseStart = marker.index + marker[0].length + 1;
    const responseEnd = markers[index + 1]?.index ?? block.length;
    const responseBlock = block.slice(responseStart, responseEnd);
    return [marker[1], {
      componentRef: responseBlock.match(/^\s+\$ref: '#\/components\/responses\/([^']+)'$/m)?.[1],
      schemaRef: responseBlock.match(/^\s+\$ref: '#\/components\/schemas\/([^']+)'$/m)?.[1],
      hasContent: /^\s+content:$/m.test(responseBlock),
    }];
  }));
}

function parseSecuritySchemes(source) {
  const section = between(source, '  securitySchemes:\n', '\n  schemas:');
  const markers = [...section.matchAll(/^    ([A-Za-z][A-Za-z0-9]*):$/gm)];
  return markers.map((marker, index) => {
    const blockStart = marker.index + marker[0].length + 1;
    const blockEnd = markers[index + 1]?.index ?? section.length;
    const block = section.slice(blockStart, blockEnd);
    return {
      name: marker[1],
      type: block.match(/^      type: ([^\n]+)$/m)?.[1],
      in: block.match(/^      in: ([^\n]+)$/m)?.[1],
      parameterName: block.match(/^      name: ([^\n]+)$/m)?.[1],
    };
  });
}

function parseComponentResponses(source) {
  const section = between(source, '  responses:\n', undefined);
  const markers = [...section.matchAll(/^    ([A-Za-z][A-Za-z0-9]*):$/gm)];
  return new Map(markers.map((marker, index) => {
    const blockStart = marker.index + marker[0].length + 1;
    const blockEnd = markers[index + 1]?.index ?? section.length;
    const block = section.slice(blockStart, blockEnd);
    return [marker[1], {
      schemaRef: block.match(/^\s+\$ref: '#\/components\/schemas\/([^']+)'$/m)?.[1],
    }];
  }));
}

function assertClientOperation(operation, functions, responses) {
  const functionName = operation.operationId === 'getAuthSession' ? 'getSession' : operation.operationId;
  const declaration = functions.get(functionName);
  if (declaration === undefined) {
    fail(`operationId ${operation.operationId} não possui função cliente correspondente`);
  }
  const callName = (functionName === 'logout' || functionName === 'deleteDraftEvent' || functionName === 'deleteTicketSector') ? 'fetch' : 'requestJson';
  const call = findCall(declaration, callName);
  const pathArgument = call?.arguments[0];
  const initArgument = call?.arguments[1];
  if (call === undefined || (!ts.isStringLiteral(pathArgument) && !ts.isTemplateExpression(pathArgument)) || !ts.isObjectLiteralExpression(initArgument)) {
    fail(`${functionName} não possui chamada HTTP analisável`);
  }
  const pathText = ts.isStringLiteral(pathArgument) ? pathArgument.text : pathArgument.head.text;
  const pathMatches = ts.isStringLiteral(pathArgument)
    ? pathArgument.text === operation.path
    : (pathArgument.head.text.length > 0 && operation.path.startsWith(pathArgument.head.text));
  if (!pathMatches) {
    fail(`${functionName} usa ${pathText}, mas o OpenAPI declara ${operation.path}`);
  }
  const method = objectStringProperty(initArgument, 'method');
  if (method !== operation.method) {
    fail(`${functionName} usa método ${method}, mas o OpenAPI declara ${operation.method}`);
  }

  const bodyProperty = objectProperty(initArgument, 'body');
  const sourceFile = declaration.getSourceFile();
  const functionText = declaration.getText(sourceFile);
  if (operation.requestSchema === undefined) {
    if (bodyProperty !== undefined) {
      fail(`${functionName} envia body não declarado no OpenAPI`);
    }
  } else {
    if (!aliases.has(operation.requestSchema)
      || !functionText.includes(`const request: ${operation.requestSchema}`)
      || !functionText.includes('body: JSON.stringify(request)')) {
      fail(`${functionName} não serializa exatamente ${operation.requestSchema}`);
    }
  }

  const returnType = promisedReturnType(declaration, sourceFile);
  const successes = [...operation.responses.entries()].filter(([status]) => status.startsWith('2'));
  if (successes.length !== 1) {
    fail(`${operation.path} deve declarar exatamente uma resposta de sucesso`);
  }
  const [successStatus, successResponse] = successes[0];
  if (returnType === 'void') {
    if (successStatus !== '204' || successResponse.hasContent || !hasExact204Guard(declaration)) {
      fail(`${functionName} deve aceitar somente 204 sem corpo`);
    }
  } else if ((successStatus !== '200' && successStatus !== '201') || successResponse.schemaRef !== returnType) {
    fail(`${functionName} retorna ${returnType}, mas a resposta ${successStatus} referencia ${successResponse.schemaRef}`);
  }

  for (const [status, response] of operation.responses) {
    if (!status.startsWith('2')) {
      const component = response.componentRef === undefined ? undefined : responses.get(response.componentRef);
      if (component?.schemaRef !== 'ApiError' && component?.schemaRef !== 'CatalogApiError' && component?.schemaRef !== 'EventApiError' && component?.schemaRef !== 'ReservationApiError' && component?.schemaRef !== 'TicketApiError') {
        fail(`resposta ${status} de ${operation.path} não referencia ApiError, CatalogApiError, EventApiError, ReservationApiError ou TicketApiError via component response`);
      }
    }
  }
}

function assertSecuritySemantics(operations, schemes) {
  const csrfCookie = uniqueScheme(schemes, 'cookie', 'XSRF-TOKEN');
  const csrfHeader = uniqueScheme(schemes, 'header', 'X-XSRF-TOKEN');
  const sessionCookie = uniqueScheme(schemes, 'cookie', 'EDT_SESSION');
  if (csrfCookie.type !== 'apiKey' || csrfHeader.type !== 'apiKey' || sessionCookie.type !== 'apiKey') {
    fail('cookies/header de autenticação devem usar security scheme apiKey');
  }

  for (const operation of operations.filter((candidate) => candidate.method === 'POST' || candidate.method === 'PUT' || candidate.method === 'DELETE')) {
    const expected = new Set([csrfCookie.name, csrfHeader.name]);
    if (operation.security.length !== 1 || !sameSet(operation.security[0], expected)) {
      fail(`${operation.path} deve exigir juntos cookie e header CSRF`);
    }
    const functionName = operation.operationId;
    const declaration = clientFunctions.get(functionName);
    if (declaration === undefined || !declaration.getText(declaration.getSourceFile()).includes('csrfHeaders()')) {
      fail(`${functionName} não envia a proteção CSRF declarada`);
    }
  }

  const session = operations.find((operation) => operation.operationId === 'getAuthSession');
  if (session === undefined
    || !session.security.some((requirement) => sameSet(requirement, new Set([sessionCookie.name])))
    || !session.security.some((requirement) => requirement.size === 0)) {
    fail('getAuthSession deve aceitar cookie de sessão ou acesso anônimo');
  }
  if (!authTypeSource.includes(`readCookie('${csrfCookie.parameterName}')`)
    || !authTypeSource.includes(`'${csrfHeader.parameterName}'`)) {
    fail('nomes CSRF do cliente divergiram dos security schemes');
  }
  if (authTypeSource.includes(sessionCookie.parameterName)
    || catalogTypeSource.includes(sessionCookie.parameterName)
    || eventsTypeSource.includes(sessionCookie.parameterName)) {
    fail('o cliente JavaScript não pode acessar EDT_SESSION');
  }
}

function findCall(declaration, callName) {
  let result;
  const visit = (node) => {
    if (result === undefined
      && ts.isCallExpression(node)
      && ts.isIdentifier(node.expression)
      && node.expression.text === callName) {
      result = node;
      return;
    }
    ts.forEachChild(node, visit);
  };
  visit(declaration);
  return result;
}

function objectProperty(object, name) {
  return object.properties
    .filter(ts.isPropertyAssignment)
    .find((property) => property.name.getText(object.getSourceFile()).replaceAll("'", '') === name);
}

function objectStringProperty(object, name) {
  const property = objectProperty(object, name);
  return property !== undefined && ts.isStringLiteral(property.initializer)
    ? property.initializer.text
    : undefined;
}

function promisedReturnType(declaration, sourceFile) {
  if (declaration.type === undefined
    || !ts.isTypeReferenceNode(declaration.type)
    || declaration.type.typeName.getText(sourceFile) !== 'Promise'
    || declaration.type.typeArguments?.length !== 1) {
    fail(`${declaration.name?.text} deve declarar retorno Promise explícito`);
  }
  const promised = declaration.type.typeArguments[0];
  return promised.kind === ts.SyntaxKind.VoidKeyword ? 'void' : normalizeType(promised, sourceFile);
}

function hasExact204Guard(declaration) {
  let found = false;
  const visit = (node) => {
    if (ts.isBinaryExpression(node)
      && node.operatorToken.kind === ts.SyntaxKind.ExclamationEqualsEqualsToken
      && ts.isPropertyAccessExpression(node.left)
      && node.left.name.text === 'status'
      && ts.isNumericLiteral(node.right)
      && node.right.text === '204') {
      found = true;
    }
    ts.forEachChild(node, visit);
  };
  visit(declaration);
  return found;
}

function uniqueScheme(schemes, location, parameterName) {
  const matches = schemes.filter((scheme) => scheme.in === location && scheme.parameterName === parameterName);
  if (matches.length !== 1) {
    fail(`security scheme ${location}/${parameterName} ausente ou ambíguo`);
  }
  return matches[0];
}

function sameSet(left, right) {
  return left.size === right.size && [...left].every((value) => right.has(value));
}

function indentedSection(source, label, indent) {
  const marker = `${' '.repeat(indent)}${label}:\n`;
  const start = source.indexOf(marker);
  if (start < 0) return undefined;
  const contentStart = start + marker.length;
  const lines = source.slice(contentStart).split('\n');
  const selected = [];
  for (const line of lines) {
    if (line !== '' && line.length - line.trimStart().length <= indent) break;
    selected.push(line);
  }
  return selected.join('\n');
}

function between(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker);
  const end = endMarker === undefined ? source.length : source.indexOf(endMarker, start);
  if (start < 0 || end < 0) {
    fail(`seção ${startMarker.trim()} não encontrada no OpenAPI`);
  }
  return source.slice(start + startMarker.length, end);
}

function parseSchemas(source) {
  const start = source.indexOf('  schemas:\n');
  const end = source.indexOf('\n  responses:', start);
  if (start < 0 || end < 0) {
    fail('components.schemas não foi encontrado no OpenAPI');
  }

  const section = source.slice(start + '  schemas:\n'.length, end);
  const markers = [...section.matchAll(/^    ([A-Za-z][A-Za-z0-9]*):$/gm)];
  return markers.map((marker, index) => {
    const blockStart = marker.index + marker[0].length + 1;
    const blockEnd = markers[index + 1]?.index ?? section.length;
    return { name: marker[1], block: section.slice(blockStart, blockEnd) };
  });
}

function parseSchema(name, block) {
  const enumMatch = block.match(/^      enum: \[([^\]]+)]$/m);
  if (enumMatch !== null) {
    return { kind: 'enum', name, values: commaList(enumMatch[1]) };
  }

  const references = [...block.matchAll(/^        - \$ref: '#\/components\/schemas\/([^']+)'$/gm)]
    .map((match) => match[1]);
  if (references.length > 0 && block.includes('      oneOf:\n')) {
    return { kind: 'union', name, members: references };
  }

  if (!block.includes('      type: object\n') || !block.includes('      additionalProperties: false\n')) {
    fail(`o schema ${name} não é um objeto fechado, enum ou união suportada`);
  }

  const requiredMatch = block.match(/^      required: \[([^\]]*)]$/m);
  if (requiredMatch === null) {
    fail(`o schema ${name} não declara required`);
  }
  const required = new Set(commaList(requiredMatch[1]));
  const propertiesStart = block.indexOf('      properties:\n');
  if (propertiesStart < 0) {
    fail(`o schema ${name} não declara properties`);
  }
  const propertiesBlock = block.slice(propertiesStart + '      properties:\n'.length);
  const markers = [...propertiesBlock.matchAll(/^        ([A-Za-z][A-Za-z0-9]*):$/gm)];
  const properties = markers.map((marker, index) => {
    const propertyStart = marker.index + marker[0].length + 1;
    const propertyEnd = markers[index + 1]?.index ?? propertiesBlock.length;
    const propertyBlock = propertiesBlock.slice(propertyStart, propertyEnd);
    return {
      name: marker[1],
      optional: !required.has(marker[1]),
      type: openApiType(propertyBlock),
    };
  });

  return { kind: 'object', name, properties };
}

function openApiType(block) {
  const reference = block.match(/^          \$ref: '#\/components\/schemas\/([^']+)'$/m)?.[1];
  if (reference !== undefined) {
    return reference;
  }

  const type = block.match(/^          type: ([A-Za-z]+)$/m)?.[1];
  if (type === 'array') {
    const item = block.match(/^            \$ref: '#\/components\/schemas\/([^']+)'$/m)?.[1];
    if (item === undefined) {
      fail(`array sem items.$ref suportado: ${block.trim()}`);
    }
    return `${item}[]`;
  }
  if (type === 'boolean') {
    return block.match(/^          const: (true|false)$/m)?.[1] ?? 'boolean';
  }
  if (type === 'string') {
    return 'string';
  }
  if (type === 'integer' || type === 'number') {
    return 'number';
  }
  fail(`tipo OpenAPI não suportado: ${block.trim()}`);
}

function assertObjectAlias(schema, alias) {
  if (!ts.isTypeLiteralNode(alias.type)) {
    fail(`${schema.name} deveria ser um object type`);
  }

  const sourceFile = alias.getSourceFile();
  const actual = alias.type.members
    .filter(ts.isPropertySignature)
    .map((property) => ({
      name: property.name.getText(sourceFile),
      optional: property.questionToken !== undefined,
      type: normalizeType(property.type, sourceFile),
    }));
  const expected = [...schema.properties].sort(byName);
  actual.sort(byName);

  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    fail(`${schema.name} divergiu: esperado ${JSON.stringify(expected)}, recebido ${JSON.stringify(actual)}`);
  }
}

function assertUnionAlias(name, expectedMembers, alias) {
  if (!ts.isUnionTypeNode(alias.type)) {
    fail(`${name} deveria ser uma união TypeScript`);
  }
  const sourceFile = alias.getSourceFile();
  const actual = alias.type.types.map((type) => normalizeType(type, sourceFile)).sort();
  const expected = [...expectedMembers].sort();
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    fail(`${name} divergiu: esperado ${expected.join(' | ')}, recebido ${actual.join(' | ')}`);
  }
}

function normalizeType(type, sourceFile) {
  if (type === undefined) {
    fail('propriedade TypeScript sem tipo explícito');
  }
  if (type.kind === ts.SyntaxKind.StringKeyword) return 'string';
  if (type.kind === ts.SyntaxKind.BooleanKeyword) return 'boolean';
  if (type.kind === ts.SyntaxKind.NumberKeyword) return 'number';
  if (ts.isTypeReferenceNode(type)) return type.typeName.getText(sourceFile);
  if (ts.isArrayTypeNode(type)) return `${normalizeType(type.elementType, sourceFile)}[]`;
  if (ts.isLiteralTypeNode(type)) return type.literal.getText(sourceFile).replaceAll("'", '');
  fail(`tipo TypeScript não suportado: ${type.getText(sourceFile)}`);
}

function commaList(value) {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function byName(left, right) {
  return left.name.localeCompare(right.name);
}

function fail(message) {
  throw new Error(`Drift no contrato de OpenAPI: ${message}`);
}
