/**
 * Token do jogador de teste guardado por aba (sessionStorage): duas abas podem
 * ser dois jogadores. O armazenamento pode falhar (modo privado, iframe), então
 * toda leitura e escrita é protegida.
 */
const KEY = "warzation.playerToken";

export function readStoredToken(): string | null {
  try {
    return sessionStorage.getItem(KEY);
  } catch {
    return null;
  }
}

export function storeToken(token: string): void {
  try {
    sessionStorage.setItem(KEY, token);
  } catch {
    // sem persistência: o jogador precisará entrar de novo ao recarregar
  }
}

export function clearStoredToken(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // nada a limpar
  }
}
