export type SubjectRef = { id: number; name: string; code: string };

export type DocInfo = {
  id: number;
  title: string;
  type: string | null;
  size: number | null;
  uploadedAt: string;
};

export type Course = {
  id: number;
  title: string;
  description: string | null;
  createdAt: string;
  subject: SubjectRef;
  professorName: string;
  documents: DocInfo[];
};