import { Suspense } from 'react';
import { LoginForm } from './login-form';

export default function LoginPage() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <Suspense fallback={<div className="text-sm text-muted-foreground">Carregando...</div>}>
        <LoginForm />
      </Suspense>
    </div>
  );
}
