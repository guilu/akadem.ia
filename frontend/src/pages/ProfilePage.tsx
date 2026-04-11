import { useEffect, useState } from 'react';
import { getMyProfile, updateMyProfile } from '../api';

const inp =
  'w-full bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-2.5 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';
const card = 'border border-secondary/25 rounded-2xl p-6';
const btnPrimary =
  'btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 flex items-center gap-2 disabled:opacity-60';

export default function ProfilePage() {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    setLoading(true);
    getMyProfile()
      .then((profile) => {
        setEmail(profile.email);
        setFirstName(profile.firstName ?? '');
        setLastName(profile.lastName ?? '');
      })
      .catch(() => {
        setErrorMsg('No se pudo cargar el perfil.');
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSuccessMsg('');
    setErrorMsg('');
    setLoading(true);
    try {
      await updateMyProfile({
        firstName: firstName.trim() || null,
        lastName: lastName.trim() || null,
      });
      setSuccessMsg('Cambios guardados correctamente.');
    } catch {
      setErrorMsg('No se pudieron guardar los cambios.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold">Mi perfil</h1>

      <div className={card}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1.5" htmlFor="profile-email">
              Correo electrónico
            </label>
            <input
              id="profile-email"
              type="email"
              className={inp + ' opacity-60 cursor-not-allowed'}
              value={email}
              readOnly
              aria-readonly="true"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1.5" htmlFor="profile-firstName">
              Nombre
            </label>
            <input
              id="profile-firstName"
              type="text"
              className={inp}
              placeholder="Tu nombre"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1.5" htmlFor="profile-lastName">
              Apellidos
            </label>
            <input
              id="profile-lastName"
              type="text"
              className={inp}
              placeholder="Tus apellidos"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
            />
          </div>

          {successMsg && (
            <p role="status" className="text-sm text-green-500">
              {successMsg}
            </p>
          )}
          {errorMsg && (
            <p role="alert" className="text-sm text-red-500">
              {errorMsg}
            </p>
          )}

          <div className="flex justify-end pt-2">
            <button type="submit" className={btnPrimary} disabled={loading}>
              {loading ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
