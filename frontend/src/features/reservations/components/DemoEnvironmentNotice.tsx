import './DemoEnvironmentNotice.css';

export type DemoEnvironmentNoticeProps = {
  className?: string;
};

export function DemoEnvironmentNotice({ className = '' }: DemoEnvironmentNoticeProps) {
  return (
    <aside
      className={`edt-demo-notice ${className}`.trim()}
      role="note"
      aria-label="Aviso de ambiente de demonstração"
      data-testid="demo-environment-notice"
    >
      <div className="edt-demo-notice__icon" aria-hidden="true">
        ℹ️
      </div>
      <div className="edt-demo-notice__content">
        <strong className="edt-demo-notice__title">Ambiente de Demonstração</strong>
        <p className="edt-demo-notice__text">
          Este checkout opera exclusivamente em modo simulado para fins de validação técnica. Nenhuma cobrança financeira real será realizada.
        </p>
      </div>
    </aside>
  );
}
