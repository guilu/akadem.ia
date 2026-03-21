import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';

export default function OAuth2CallbackPage({ onToken }: { onToken: (t: string) => void }) {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    if (token) {
      onToken(token);
    } else {
      navigate(ROUTES.login, { replace: true });
    }
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-text/50 text-sm">Iniciando sesión...</p>
    </div>
  );
}
