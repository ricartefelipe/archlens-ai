'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useSearchParams } from 'next/navigation';
import { Send, Loader2, MessageSquare } from 'lucide-react';
import Link from 'next/link';
import { askQuestion, listQuestions, listAnalyses } from '@/lib/api';
import { getTenantId } from '@/lib/auth';
import { ChatMessage } from '@/components/chat-message';
import type { Question } from '@/lib/types';

export default function ChatPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const projectId = params.id as string;
  const [questions, setQuestions] = useState<Question[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(true);
  const [analysisId, setAnalysisId] = useState<string | null>(null);
  const [noAnalysis, setNoAnalysis] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const resolveAnalysisId = useCallback(async (): Promise<string | null> => {
    const fromQuery = searchParams.get('analysisId');
    if (fromQuery) return fromQuery;

    const analyses = await listAnalyses(getTenantId(), projectId);
    const completed = analyses.find((a) => a.status === 'COMPLETED');
    const sorted = [...analyses].sort(
      (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    );
    const chosen = completed ?? sorted[0];
    return chosen?.id ?? null;
  }, [projectId, searchParams]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const aid = await resolveAnalysisId();
        if (cancelled) return;
        if (!aid) {
          setNoAnalysis(true);
          setQuestions([]);
          return;
        }
        setAnalysisId(aid);
        const list = await listQuestions(getTenantId(), projectId, aid);
        if (!cancelled) setQuestions(list);
      } catch (e) {
        console.error(e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [projectId, resolveAnalysisId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [questions, sending]);

  async function handleSend(e: React.FormEvent) {
    e.preventDefault();
    const q = input.trim();
    if (!q || sending || !analysisId) return;

    setInput('');
    setSending(true);

    const tempQuestion: Question = {
      id: `temp-${Date.now()}`,
      analysisId,
      question: q,
      answer: '',
      sources: null,
      createdAt: new Date().toISOString(),
    };
    setQuestions((prev) => [...prev, tempQuestion]);

    try {
      const result = await askQuestion(getTenantId(), projectId, analysisId, q);
      setQuestions((prev) =>
        prev.map((item) => (item.id === tempQuestion.id ? result : item))
      );
    } catch (err) {
      console.error('Failed to ask question:', err);
      setQuestions((prev) =>
        prev.map((item) =>
          item.id === tempQuestion.id
            ? { ...item, answer: 'Não foi possível obter resposta. Tente novamente.' }
            : item
        )
      );
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="flex flex-col h-full">
      <div className="border-b border-border px-6 py-4">
          <h1 className="text-lg font-semibold text-foreground flex items-center gap-2">
          <MessageSquare className="w-5 h-5 text-primary" />
          Chat arquitetural
        </h1>
        <p className="text-xs text-muted-foreground mt-0.5">
          Perguntas usam RAG sobre os artefatos do projeto (via análise selecionada).
          {process.env.NEXT_PUBLIC_LLM_PROVIDER !== 'openai'
            && process.env.NEXT_PUBLIC_LLM_PROVIDER !== 'ollama' && (
            <> Modo piloto: respostas montadas a partir dos trechos indexados.</>
          )}
        </p>
        {analysisId && (
          <p className="text-xs text-muted-foreground mt-1 font-mono truncate">
            Análise: {analysisId}
          </p>
        )}
      </div>

      <div className="flex-1 overflow-auto px-6 py-4 space-y-4">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-6 h-6 text-primary animate-spin" />
          </div>
        ) : noAnalysis || !analysisId ? (
          <div className="flex flex-col items-center justify-center py-20 text-center max-w-md mx-auto">
            <MessageSquare className="w-12 h-12 text-muted-foreground mb-4" />
            <p className="text-foreground font-medium">Nenhuma análise disponível</p>
            <p className="text-sm text-muted-foreground mt-1 mb-4">
              Crie e conclua uma análise neste projeto para fazer perguntas no chat.
            </p>
            <Link
              href={`/projects/${projectId}`}
              className="text-sm text-primary hover:underline"
            >
              Voltar ao projeto
            </Link>
          </div>
        ) : questions.length === 0 && !sending ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <MessageSquare className="w-12 h-12 text-muted-foreground mb-4" />
            <p className="text-foreground font-medium">Faça uma pergunta sobre a arquitetura</p>
            <p className="text-sm text-muted-foreground mt-1">
              As respostas usam trechos do código como evidência quando disponíveis.
            </p>
          </div>
        ) : (
          questions.map((q) => (
            <div key={q.id} className="space-y-3">
              <ChatMessage
                type="user"
                content={q.question}
                timestamp={q.createdAt}
              />
              {q.answer ? (
                <ChatMessage
                  type="assistant"
                  content={q.answer}
                  sources={q.sources ?? undefined}
                  timestamp={q.createdAt}
                />
              ) : (
                <ChatMessage
                  type="assistant"
                  content=""
                  timestamp={q.createdAt}
                  loading
                />
              )}
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={handleSend} className="border-t border-border p-4">
        <div className="flex gap-3 max-w-4xl mx-auto">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Pergunte sobre riscos, API, Docker, migrations..."
            disabled={sending || !analysisId || loading}
            className="flex-1 px-4 py-2.5 bg-card border border-border rounded-xl text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={sending || !input.trim() || !analysisId || loading}
            className="px-4 py-2.5 bg-primary text-primary-foreground rounded-xl text-sm font-medium hover:bg-primary/80 disabled:opacity-50 transition-colors"
          >
            {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
          </button>
        </div>
      </form>
    </div>
  );
}
