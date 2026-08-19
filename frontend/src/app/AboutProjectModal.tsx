import { useEffect, useRef } from 'react';
import './AboutProjectModal.css';

export type AboutProjectModalProps = {
  isOpen: boolean;
  onClose: () => void;
  triggerRef?: React.RefObject<HTMLElement | null>;
};

export function AboutProjectModal({ isOpen, onClose, triggerRef }: AboutProjectModalProps) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const triggerElementRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (isOpen) {
      triggerElementRef.current = triggerRef?.current ?? (document.activeElement as HTMLElement | null);
      closeButtonRef.current?.focus();

      const handleKeyDown = (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
          event.preventDefault();
          onClose();
        }
      };

      window.addEventListener('keydown', handleKeyDown);
      return () => {
        window.removeEventListener('keydown', handleKeyDown);
        triggerElementRef.current?.focus();
      };
    }
  }, [isOpen, onClose, triggerRef]);

  if (!isOpen) {
    return null;
  }

  const handleBackdropClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className="tc-about-modal-backdrop"
      role="presentation"
      onClick={handleBackdropClick}
    >
      <div
        className="tc-about-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="about-modal-title"
      >
        <div className="tc-about-modal__header">
          <span className="tc-about-modal__label">DESAFIO / 2026</span>
          <button
            ref={closeButtonRef}
            type="button"
            className="tc-about-modal__close-btn"
            onClick={onClose}
            aria-label="Fechar modal Sobre o Projeto"
          >
            FECHAR ×
          </button>
        </div>

        <div className="tc-about-modal__title-group">
          <h2 id="about-modal-title" className="tc-about-modal__title">
            SOBRE O<br />PROJETO
          </h2>
          <span className="tc-about-modal__underline" aria-hidden="true" />
        </div>

        <div className="tc-about-modal__body">
          <p>
            Esse projeto foi, sem dúvida, um dos mais difíceis que já enfrentei. Foram dias de muito aprendizado, madrugadas tentando resolver bugs, bastante estresse para acertar a UI e vários momentos em que achei que não daria tempo.
          </p>
          <p>
            Mesmo assim, tentei cuidar de cada pedaço para entregar algo que realmente mostrasse meu esforço, mesmo não conseguindo colocar tudo que desejava.
          </p>
          <p>
            No fim, mais do que simplesmente concluir o desafio, minha maior preocupação foi entregar aos avaliadores o melhor projeto que eu conseguia fazer.
          </p>
        </div>
      </div>
    </div>
  );
}
