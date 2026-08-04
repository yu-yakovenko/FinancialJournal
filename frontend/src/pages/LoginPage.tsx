import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export function LoginPage() {
  const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.login(password);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="login-page">
      <form className="modal" onSubmit={handleSubmit}>
        <h3>Вхід</h3>
        {error && <div className="error-banner">{error}</div>}
        <div className="form-row">
          <label htmlFor="password">Пароль</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoFocus
            required
          />
        </div>
        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>
            {saving ? 'Вхід…' : 'Увійти'}
          </button>
        </div>
      </form>
    </div>
  );
}
