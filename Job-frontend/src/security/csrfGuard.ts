/**
 * CSRF Guard — generates and attaches X-CSRF-Token header
 * on state-changing requests (POST, PUT, PATCH, DELETE).
 * Defense-in-depth measure.
 */

let csrfToken: string | null = null;

function generateToken(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return Array.from(array, (b) => b.toString(16).padStart(2, '0')).join('');
}

export const csrfGuard = {
  getToken(): string {
    if (!csrfToken) {
      csrfToken = generateToken();
    }
    return csrfToken;
  },

  shouldAttach(method: string): boolean {
    const stateChanging = ['post', 'put', 'patch', 'delete'];
    return stateChanging.includes(method.toLowerCase());
  },

  getHeaderName(): string {
    return 'X-CSRF-Token';
  },
};
