export type Subject = { id: string; name: string; description?: string };
export type UnitAvailability = { id: string; name: string; available: number };
export type ExamStartResponse = { attemptId: string; totalTimeSeconds: number; questions: any[] };
export type ExamResult = {
  total: number;
  correct: number;
  wrong: number;
  penalty: number;
  net: number;
  percentage: number;
};

export type ExamAttemptSummary = {
  attemptId: string;
  subjectName: string;
  startedAt: string;
  finishedAt?: string | null;
  totalTimeSeconds: number;
  score?: number | null;
  totalQuestions: number;
  correctCount: number;
  wrongCount: number;
  percent: number;
};

// RAG module types
export type SourceDocument = {
  id: string;
  subjectId?: string | null;
  name: string;
  type: string;
  status: 'UPLOADED' | 'PENDING_REVIEW' | 'PROCESSED' | 'FAILED';
  uploadedAt: string;
};

export type DetectedUnit = {
  name: string;
  headingKey: string;
  chunkCount: number;
};

export type IndexPreview = {
  document: SourceDocument;
  detectedUnits: DetectedUnit[];
};

export type ApprovedUnit = {
  headingKey: string;
  name: string;
  description?: string;
};

export type ConfirmIndexCommand = {
  approvedUnits: ApprovedUnit[];
};

export type GeneratedDraft = {
  id: string;
  sourceDocumentId: string;
  unitId?: string | null;
  topic: string;
  difficulty: string;
  statement: string;
  answers: string[];
  correctIndex: number;
  hint?: string | null;
  explanation?: string | null;
  reference?: string | null;
  status: 'GENERATED' | 'VALIDATED' | 'REJECTED';
};

export type GenerateQuizCommand = {
  sourceId: string;
  unitId: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  questionCount: number;
  includeHints: boolean;
  storeAsDraft: boolean;
};

export type GenerateQuizResponse = {
  generated: number;
  questions: GeneratedDraft[];
};

export type AdminUnit = { id: string; name: string };

export interface NavUser {
  email: string;
  initials: string;
  avatarUrl?: string;
}

export interface UserProfile {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
}
