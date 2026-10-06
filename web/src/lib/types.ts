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

export type Timetable = {
  id: number;
  academicYear: string;
  semester: string;
  sessionCount: number;
};

export type Session = {
  id: number;
  timetableId: number;
  academicYear: string;
  semester: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  room: string | null;
  classId: number;
  className: string;
  subjectId: number;
  subjectName: string;
  subjectCode: string;
  professorId: number;
  professorName: string;
};