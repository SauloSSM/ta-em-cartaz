import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Footer } from './Footer';

describe('Footer component', () => {
  it('renderiza a marca Tá em Cartaz e o identificador oficial', () => {
    render(<Footer />);

    expect(screen.getByText('TÁ EM CARTAZ')).toBeDefined();
    expect(screen.getByText('@TAEMCARTAZ.BR')).toBeDefined();
  });

  it('renderiza apenas Sobre o Projeto, GitHub e LinkedIn, sem links legados', () => {
    render(<Footer />);

    // Links presentes
    expect(screen.getByRole('button', { name: 'Sobre o Projeto' })).toBeDefined();

    const githubLink = screen.getByRole('link', { name: 'GitHub' });
    expect(githubLink.getAttribute('href')).toBe('https://github.com/SauloSSM');
    expect(githubLink.getAttribute('target')).toBe('_blank');
    expect(githubLink.getAttribute('rel')).toBe('noopener noreferrer');

    const linkedinLink = screen.getByRole('link', { name: 'LinkedIn' });
    expect(linkedinLink.getAttribute('href')).toBe(
      'https://www.linkedin.com/in/saulo-da-silva-stuque-menegucci/',
    );
    expect(linkedinLink.getAttribute('target')).toBe('_blank');
    expect(linkedinLink.getAttribute('rel')).toBe('noopener noreferrer');

    // Links removidos
    expect(screen.queryByText('Eventos')).toBeNull();
    expect(screen.queryByText('Minha Conta')).toBeNull();
    expect(screen.queryByText('Termos de Uso')).toBeNull();
    expect(screen.queryByText('Privacidade')).toBeNull();
  });

  it('abre o modal "Sobre o Projeto" com título, label e parágrafos editoriais ao clicar no botão', async () => {
    const user = userEvent.setup();
    render(<Footer />);

    expect(screen.queryByRole('dialog')).toBeNull();

    const aboutBtn = screen.getByRole('button', { name: 'Sobre o Projeto' });
    await user.click(aboutBtn);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeDefined();
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(screen.getByText('DESAFIO / 2026')).toBeDefined();
    expect(screen.getByRole('heading', { level: 2, name: /SOBRE O\s*PROJETO/i })).toBeDefined();

    expect(
      screen.getByText(/Esse projeto foi, sem dúvida, um dos mais difíceis que já enfrentei/),
    ).toBeDefined();
    expect(
      screen.getByText(/Mesmo assim, tentei cuidar de cada pedaço para entregar algo que realmente mostrasse meu esforço/),
    ).toBeDefined();
    expect(
      screen.getByText(/No fim, mais do que simplesmente concluir o desafio, minha maior preocupação/),
    ).toBeDefined();
  });

  it('fecha o modal pelo botão "FECHAR ×" e devolve o foco para o botão acionador', async () => {
    const user = userEvent.setup();
    render(<Footer />);

    const aboutBtn = screen.getByRole('button', { name: 'Sobre o Projeto' });
    await user.click(aboutBtn);

    const closeBtn = screen.getByRole('button', { name: 'Fechar modal Sobre o Projeto' });
    expect(document.activeElement).toBe(closeBtn);

    await user.click(closeBtn);

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(aboutBtn);
  });

  it('fecha o modal ao pressionar a tecla Escape e devolve o foco para o botão acionador', async () => {
    const user = userEvent.setup();
    render(<Footer />);

    const aboutBtn = screen.getByRole('button', { name: 'Sobre o Projeto' });
    await user.click(aboutBtn);

    expect(screen.getByRole('dialog')).toBeDefined();

    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(aboutBtn);
  });

  it('fecha o modal ao clicar no backdrop e não fecha ao clicar dentro do conteúdo', async () => {
    const user = userEvent.setup();
    render(<Footer />);

    const aboutBtn = screen.getByRole('button', { name: 'Sobre o Projeto' });
    await user.click(aboutBtn);

    // Clicar dentro do modal não fecha
    const dialog = screen.getByRole('dialog');
    await user.click(dialog);
    expect(screen.getByRole('dialog')).toBeDefined();

    // Clicar no backdrop fecha
    const backdrop = screen.getByRole('presentation');
    await user.click(backdrop);

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(aboutBtn);
  });
});
