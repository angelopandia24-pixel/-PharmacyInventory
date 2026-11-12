import { useEffect, useState } from 'react';
import { http } from '../shared/api/httpClient';

export default function App() {
  const [estado, setEstado] = useState('consultando...');
  useEffect(() => {
    http<{ status: string; servicio: string }>('/api/v1/health')
      .then((r) => setEstado(`${r.servicio}: ${r.status}`))
      .catch((e) => setEstado(`sin conexión (${e.message})`));
  }, []);
  return (
    <div>
      <p>{estado}</p>
    </div>
  );
}