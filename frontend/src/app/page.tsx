import Link from 'next/link';
import { ArrowRight, FileSearch, MessageSquare, Shield } from 'lucide-react';
import { APP_NAME, SUPPORT_URL } from '@/lib/branding';

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="border-b border-border">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <span className="text-lg font-semibold">{APP_NAME}</span>
          <Link
            href="/login"
            className="text-sm font-medium text-primary hover:underline"
          >
            Entrar
          </Link>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-16 space-y-16">
        <section className="space-y-6">
          <p className="text-sm font-medium text-primary uppercase tracking-wide">
            Diagnóstico arquitetural
          </p>
          <h1 className="text-4xl md:text-5xl font-bold leading-tight max-w-3xl">
            Evidências rastreáveis, relatório executivo e chat sobre o seu código
          </h1>
          <p className="text-lg text-muted-foreground max-w-2xl">
            Upload do repositório, análise estática multi-stack, ADRs sugeridos e exportação
            consultiva — pronto para follow-up com o time e com o cliente.
          </p>
          <Link
            href="/login"
            className="inline-flex items-center gap-2 px-6 py-3 bg-primary text-primary-foreground rounded-xl text-sm font-medium hover:bg-primary/90 transition-colors"
          >
            Acessar plataforma
            <ArrowRight className="w-4 h-4" />
          </Link>
        </section>

        <section className="grid md:grid-cols-3 gap-6">
          <div className="rounded-xl border border-border bg-card p-6 space-y-3">
            <FileSearch className="w-8 h-8 text-primary" />
            <h2 className="font-semibold">Análise com evidência</h2>
            <p className="text-sm text-muted-foreground">
              Riscos ligados a arquivo, trecho e recomendação — Java, Python, TS, Docker, OpenAPI, Terraform e mais.
            </p>
          </div>
          <div className="rounded-xl border border-border bg-card p-6 space-y-3">
            <Shield className="w-8 h-8 text-primary" />
            <h2 className="font-semibold">Relatório executivo</h2>
            <p className="text-sm text-muted-foreground">
              Sumário para decisores, matriz de severidade e exportação MD/JSON/PDF white-label.
            </p>
          </div>
          <div className="rounded-xl border border-border bg-card p-6 space-y-3">
            <MessageSquare className="w-8 h-8 text-primary" />
            <h2 className="font-semibold">Chat arquitetural</h2>
            <p className="text-sm text-muted-foreground">
              Perguntas sobre achados com contexto indexado do projeto (RAG).
            </p>
          </div>
        </section>

        {SUPPORT_URL && (
          <p className="text-sm text-muted-foreground">
            Consultoria e upgrade de plano:{' '}
            <a href={SUPPORT_URL} className="text-primary hover:underline">
              fale conosco
            </a>
          </p>
        )}
      </main>
    </div>
  );
}
