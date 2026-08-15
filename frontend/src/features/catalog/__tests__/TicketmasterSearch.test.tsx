import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TicketmasterSearch } from '../components/TicketmasterSearch';
import { TicketmasterResultCard } from '../components/TicketmasterResultCard';

const originalFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = originalFetch;
});

describe('TicketmasterResultCard', () => {
  it('renderiza título, categoria, descrição e botão de referência', () => {
    const onSelect = vi.fn();
    render(
      <TicketmasterResultCard
        event={{
          externalId: 'tm-1',
          title: 'Festival de Inverno',
          description: 'Apresentações musicais na serra',
          category: 'Música',
          imageUrl: 'https://images.example.com/fest.jpg',
        }}
        onSelectReference={onSelect}
      />,
    );

    expect(screen.getByRole('heading', { level: 3, name: 'Festival de Inverno' })).toBeDefined();
    expect(screen.getByText('Música')).toBeDefined();
    expect(screen.getByText('Apresentações musicais na serra')).toBeDefined();
    const image = screen.getByRole('img', { name: 'Banner do evento Festival de Inverno' });
    expect(image).toBeDefined();
    expect(image.getAttribute('src')).toBe('https://images.example.com/fest.jpg');

    const button = screen.getByRole('button', { name: 'Usar Festival de Inverno como referência' });
    expect(button).toBeDefined();
    fireEvent.click(button);
    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ externalId: 'tm-1', title: 'Festival de Inverno' }),
    );
  });

  it('exibe fallback visual quando imageUrl não for fornecida ou falhar ao carregar', () => {
    const { rerender } = render(
      <TicketmasterResultCard
        event={{
          externalId: 'tm-2',
          title: 'Evento Sem Imagem',
        }}
        onSelectReference={vi.fn()}
      />,
    );

    expect(screen.getByText('Sem imagem')).toBeDefined();
    expect(screen.queryByRole('img')).toBeNull();

    rerender(
      <TicketmasterResultCard
        event={{
          externalId: 'tm-3',
          title: 'Evento Imagem Quebrada',
          imageUrl: 'https://images.example.com/broken.jpg',
        }}
        onSelectReference={vi.fn()}
      />,
    );

    const img = screen.getByRole('img');
    fireEvent.error(img);

    expect(screen.getByText('Sem imagem')).toBeDefined();
  });
});

describe('TicketmasterSearch (Superfície S10)', () => {
  it('executa pesquisa, exibe loading acessível e renderiza os resultados', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({
        events: [
          {
            externalId: 'tm-10',
            title: 'Rock in Rio 2026',
            description: 'Maior festival de música',
            category: 'Rock',
            imageUrl: 'https://images.example.com/rock.jpg',
          },
          {
            externalId: 'tm-11',
            title: 'Teatro Municipal',
            category: 'Teatro',
          },
        ],
      }),
    );
    globalThis.fetch = fetchMock;

    render(<TicketmasterSearch />);

    const searchInput = screen.getByLabelText('Palavra-chave do evento');
    const searchButton = screen.getByRole('button', { name: 'Buscar referências' });

    await user.type(searchInput, 'Rock');
    await user.click(searchButton);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/catalog/events?keyword=Rock',
      expect.objectContaining({ method: 'GET' }),
    );

    expect(await screen.findByRole('heading', { level: 3, name: 'Rock in Rio 2026' })).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Teatro Municipal' })).toBeDefined();
  });

  it('exibe mensagem acessível de estado vazio quando nenhum evento for encontrado', async () => {
    const user = userEvent.setup();
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({ events: [] }),
    );

    render(<TicketmasterSearch />);

    const searchInput = screen.getByLabelText('Palavra-chave do evento');
    await user.type(searchInput, 'Inexistente');
    await user.click(screen.getByRole('button', { name: 'Buscar referências' }));

    expect(
      await screen.findByText('Nenhum evento encontrado no catálogo para a busca realizada.'),
    ).toBeDefined();
  });

  it('trata indisponibilidade do catálogo com alerta, sem criação manual, e permite nova tentativa preservando o termo', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(
        jsonResponse(
          {
            code: 'CATALOG_UNAVAILABLE',
            message: 'Catálogo Ticketmaster temporariamente indisponível.',
            traceId: 'trace-1',
            timestamp: '2026-08-15T12:00:00Z',
          },
          503,
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          events: [
            {
              externalId: 'tm-recuperado',
              title: 'Show Recuperado',
            },
          ],
        }),
      );
    globalThis.fetch = fetchMock;

    render(<TicketmasterSearch />);

    const searchInput = screen.getByLabelText('Palavra-chave do evento') as HTMLInputElement;
    await user.type(searchInput, 'Show');
    await user.click(screen.getByRole('button', { name: 'Buscar referências' }));

    const alert = await screen.findByRole('alert');
    expect(alert.textContent).toContain('Catálogo Ticketmaster temporariamente indisponível.');

    // Não deve oferecer criação manual em caso de indisponibilidade
    expect(screen.queryByText(/criar manualmente/i)).toBeNull();
    expect(screen.queryByRole('button', { name: /criar manualmente/i })).toBeNull();

    // Botão tentar novamente deve estar disponível
    const retryBtn = screen.getByRole('button', { name: 'Tentar novamente' });
    expect(retryBtn).toBeDefined();

    // Clica em Tentar novamente
    await user.click(retryBtn);

    // Deve preservar o termo digitado
    expect(searchInput.value).toBe('Show');

    // Deve ter chamado a API novamente
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(await screen.findByRole('heading', { level: 3, name: 'Show Recuperado' })).toBeDefined();
  });

  it('permite selecionar referência com anúncio acessível sem persistir evento DRAFT', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    globalThis.fetch = vi.fn<typeof fetch>().mockResolvedValueOnce(
      jsonResponse({
        events: [
          {
            externalId: 'tm-ref-1',
            title: 'Concerto Sinfônico',
            category: 'Clássica',
          },
        ],
      }),
    );

    render(<TicketmasterSearch onSelectReference={onSelect} />);

    await user.type(screen.getByLabelText('Palavra-chave do evento'), 'Concerto');
    await user.click(screen.getByRole('button', { name: 'Buscar referências' }));

    const selectButton = await screen.findByRole('button', {
      name: 'Usar Concerto Sinfônico como referência',
    });
    await user.click(selectButton);

    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({
        externalId: 'tm-ref-1',
        title: 'Concerto Sinfônico',
      }),
    );

    // Anúncio e feedback de seleção na tela
    expect(screen.getByText('Referência selecionada:')).toBeDefined();
    expect(screen.getByText('(Criação de rascunho disponível na próxima etapa)')).toBeDefined();
    expect(screen.getByRole('heading', { level: 3, name: 'Concerto Sinfônico' })).toBeDefined();
    expect(screen.getAllByText(/Concerto Sinfônico/)).toHaveLength(3);
  });
});

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}
