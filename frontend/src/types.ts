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
