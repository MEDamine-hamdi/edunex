// Typed views over the backend DTOs (record shapes read from the Spring source).

typedef JMap = Map<String, dynamic>;

String? str(dynamic v) => v?.toString();
int? asInt(dynamic v) => v is num ? v.toInt() : (v is String ? int.tryParse(v) : null);
double? asDouble(dynamic v) => v is num ? v.toDouble() : (v is String ? double.tryParse(v) : null);
DateTime? asDate(dynamic v) => v == null ? null : DateTime.tryParse(v.toString());
List<T> listOf<T>(dynamic v, T Function(JMap) f) =>
    (v as List? ?? const []).map((e) => f(Map<String, dynamic>.from(e as Map))).toList();
List<JMap> maps(dynamic v) => (v as List? ?? const []).map((e) => Map<String, dynamic>.from(e as Map)).toList();

class SessionItem {
  final int id;
  final String dayOfWeek, startTime, endTime;
  final String? room, className, subjectName, subjectCode, professorName, academicYear, semester;
  final int? classId, subjectId, professorId;
  SessionItem.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        dayOfWeek = str(j['dayOfWeek']) ?? '',
        startTime = str(j['startTime']) ?? '',
        endTime = str(j['endTime']) ?? '',
        room = str(j['room']),
        className = str(j['className']),
        subjectName = str(j['subjectName']),
        subjectCode = str(j['subjectCode']),
        professorName = str(j['professorName']),
        academicYear = str(j['academicYear']),
        semester = str(j['semester']),
        classId = asInt(j['classId']),
        subjectId = asInt(j['subjectId']),
        professorId = asInt(j['professorId']);
}

class WeekSession {
  final int sessionId;
  final DateTime? date;
  final String dayOfWeek, startTime, endTime;
  final String? room, className, subjectName, subjectCode, professorName;
  final int? classId, subjectId, professorId;
  final JMap? absence;
  final bool canDeclare;
  WeekSession.fromJson(JMap j)
      : sessionId = asInt(j['sessionId']) ?? 0,
        date = asDate(j['date']),
        dayOfWeek = str(j['dayOfWeek']) ?? '',
        startTime = str(j['startTime']) ?? '',
        endTime = str(j['endTime']) ?? '',
        room = str(j['room']),
        className = str(j['className']),
        subjectName = str(j['subjectName']),
        subjectCode = str(j['subjectCode']),
        professorName = str(j['professorName']),
        classId = asInt(j['classId']),
        subjectId = asInt(j['subjectId']),
        professorId = asInt(j['professorId']),
        absence = j['absence'] == null ? null : Map<String, dynamic>.from(j['absence'] as Map),
        canDeclare = j['canDeclare'] == true;
  int? get absenceId => asInt(absence?['id']);
}

class DocInfo {
  final int id;
  final String title, type;
  final int? size;
  final DateTime? uploadedAt;
  DocInfo.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        title = str(j['title']) ?? '',
        type = str(j['type']) ?? '',
        size = asInt(j['size']),
        uploadedAt = asDate(j['uploadedAt']);
}

class Course {
  final int id;
  final String title;
  final String? description, subjectName, subjectCode, professorName;
  final int? subjectId;
  final DateTime? createdAt;
  final List<DocInfo> documents;
  Course.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        title = str(j['title']) ?? '',
        description = str(j['description']),
        createdAt = asDate(j['createdAt']),
        subjectId = asInt((j['subject'] as Map?)?['id']),
        subjectName = str((j['subject'] as Map?)?['name']),
        subjectCode = str((j['subject'] as Map?)?['code']),
        professorName = str(j['professorName']),
        documents = listOf(j['documents'], DocInfo.fromJson);
}

class SubjectRef {
  final int id;
  final String name;
  final String? code;
  SubjectRef.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        name = str(j['name']) ?? '',
        code = str(j['code']);
  String get label => code == null || code!.isEmpty ? name : '$name ($code)';
}

class Grade {
  final int id;
  final String? studentName, studentNumber, subjectName, subjectCode, evaluationType;
  final int? studentId, subjectId;
  final double? value;
  final DateTime? date;
  Grade.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        studentId = asInt(j['studentId']),
        studentName = str(j['studentName']),
        studentNumber = str(j['studentNumber']),
        subjectId = asInt(j['subjectId']),
        subjectName = str(j['subjectName']),
        subjectCode = str(j['subjectCode']),
        value = asDouble(j['value']),
        evaluationType = str(j['evaluationType']),
        date = asDate(j['date']);
}

class AttendanceRecord {
  final int id;
  final int? studentId, subjectId;
  final String? studentName, studentNumber, subjectName, status, justification;
  final DateTime? date;
  AttendanceRecord.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        studentId = asInt(j['studentId']),
        studentName = str(j['studentName']),
        studentNumber = str(j['studentNumber']),
        subjectId = asInt(j['subjectId']),
        subjectName = str(j['subjectName']),
        date = asDate(j['date']),
        status = str(j['status']),
        justification = str(j['justification']);
}

class NotificationItem {
  final int id;
  final String message, type;
  final bool read;
  final DateTime? sentAt;
  NotificationItem.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        message = str(j['message']) ?? '',
        type = str(j['type']) ?? '',
        read = j['read'] == true,
        sentAt = asDate(j['sentAt']);
}

class Attachment {
  final int id;
  final String name;
  final String? contentType;
  final int? size;
  final bool image;
  Attachment.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        name = str(j['name']) ?? '',
        contentType = str(j['contentType']),
        size = asInt(j['size']),
        image = j['image'] == true;
}

class Announcement {
  final int id;
  final String title, content;
  final DateTime? publishedAt;
  final String? audience, className, publishedBy;
  final int? recipients;
  final List<Attachment> attachments;
  Announcement.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        title = str(j['title']) ?? '',
        content = str(j['content']) ?? '',
        publishedAt = asDate(j['publishedAt']),
        audience = str(j['audience']),
        className = str(j['className']),
        publishedBy = str(j['publishedBy']),
        recipients = asInt(j['recipients']),
        attachments = listOf(j['attachments'], Attachment.fromJson);
}

class SourceRef {
  final int n;
  final int? documentId;
  final String? documentTitle, courseTitle, subjectName, kind;
  SourceRef.fromJson(JMap j)
      : n = asInt(j['n']) ?? 0,
        documentId = asInt(j['documentId']),
        documentTitle = str(j['documentTitle']),
        courseTitle = str(j['courseTitle']),
        subjectName = str(j['subjectName']),
        kind = str(j['kind']);
}

class ChatMessage {
  final int? id;
  final String role, content;
  final DateTime? createdAt;
  final List<SourceRef> sources;
  final String? attachmentName;
  ChatMessage.fromJson(JMap j)
      : id = asInt(j['id']),
        role = str(j['role']) ?? 'USER',
        content = str(j['content']) ?? '',
        createdAt = asDate(j['createdAt']),
        sources = listOf(j['sources'], SourceRef.fromJson),
        attachmentName = str(j['attachmentName']);
  bool get isUser => role.toUpperCase() == 'USER';
}

class ChatSession {
  final int id;
  final String title;
  final DateTime? updatedAt;
  ChatSession.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        title = str(j['title']) ?? 'Conversation',
        updatedAt = asDate(j['updatedAt']);
}

class RequestView {
  final int id, copies;
  final String type, status;
  final String? reason, details, adminNote, handledBy, requesterName, requesterRole, requesterEmail, fileName;
  final DateTime? createdAt, updatedAt;
  final bool hasFile;
  RequestView.fromJson(JMap j)
      : id = asInt(j['id']) ?? 0,
        copies = asInt(j['copies']) ?? 1,
        type = str(j['type']) ?? '',
        status = str(j['status']) ?? 'FILED',
        reason = str(j['reason']),
        details = str(j['details']),
        adminNote = str(j['adminNote']),
        handledBy = str(j['handledBy']),
        requesterName = str(j['requesterName']),
        requesterRole = str(j['requesterRole']),
        requesterEmail = str(j['requesterEmail']),
        createdAt = asDate(j['createdAt']),
        updatedAt = asDate(j['updatedAt']),
        hasFile = j['hasFile'] == true,
        fileName = str(j['fileName']);
}

const requestStatusFr = {'FILED': 'Déposée', 'IN_PROGRESS': 'En cours', 'READY_FOR_PICKUP': 'Prête à retirer'};
