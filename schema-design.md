# Schema Design — Smart Clinic Management System

Dual-database design: **MySQL** (`cms`) for the structured, transactional core
(patients, doctors, appointments, admin) and **MongoDB** (`prescriptions`) for
flexible, evolving medical documents. Justifications appear inline as notes.

---

## MySQL Database Design

Engine: InnoDB (required for foreign keys), charset `utf8mb4`.
IDs are `INT AUTO_INCREMENT` because JPA entities use `Long` identity IDs
(matches the `mysql_table_example.md` format; simpler than UUIDs for a course-scale app).

### Table: admin
- id: INT, Primary Key, Auto Increment
- username: VARCHAR(50), Not Null, Unique — login identifier (JWT subject for admins)
- password: VARCHAR(255), Not Null — sized for a future BCrypt hash even though the current baseline stores it in clear

### Table: doctors
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null — length 3–100 validated in code (Bean Validation), not in SQL
- specialty: VARCHAR(50), Not Null
- email: VARCHAR(100), Not Null, Unique — duplicate doctor email → 409 on creation; `@Email` format check in code
- password: VARCHAR(255), Not Null — same rationale as `admin.password`
- phone: VARCHAR(10), Not Null, Unique — stored as VARCHAR to keep leading zeros; exactly-10-digit format (`^[0-9]{10}$`) validated in code, DB only enforces length

### Table: patients
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(100), Not Null
- email: VARCHAR(100), Not Null, Unique — registration rejects a duplicate email; format validated in code (`@Email`)
- password: VARCHAR(255), Not Null
- phone: VARCHAR(10), Not Null, Unique — registration also rejects a duplicate phone; format validated in code
- address: VARCHAR(255), Nullable — optional contact data

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctors(id), Not Null, On Delete Cascade
- patient_id: INT, Foreign Key → patients(id), Not Null, On Delete Restrict — default RESTRICT: history must outlive everything else (see decision 1)
- appointment_time: DATETIME, Not Null — future-ness (`@Future`) checked in code on creation
- status: INT, Not Null, Default 0 (0 = Scheduled, 1 = Completed, 2 = Cancelled) — 0/1 are the values used by the current contract; 2 is reserved for an explicit cancel state
- Unique Key (doctor_id, appointment_time) — hard guarantee against double-booking (see decision 3)

### Table: doctor_available_times
*(collection table for the `Doctor.availableTimes` `@ElementCollection` — each doctor owns their slots)*
- doctor_id: INT, Foreign Key → doctors(id), Not Null, On Delete Cascade — deleting a doctor removes their slots with no orphan rows
- slot: VARCHAR(11), Not Null — format `"HH:mm-HH:mm"`, e.g. `"09:00-10:00"`; an appointment must start exactly at a slot's start time
- Primary Key (doctor_id, slot) — composite PK prevents duplicated slots for the same doctor

> **Not included: `payments`, `clinic_locations`.** The system runs a single
> clinic and has no billing feature in scope (YAGNI); both can be added later
> with their own migrations without touching the tables above.

### Design decisions and justifications

1. **What happens if a patient is deleted? Should appointments also be deleted?**
   Patients are never hard-deleted: the API exposes no patient-delete operation,
   and appointment history is the clinical record of record. The FK uses
   `ON DELETE RESTRICT` so even manual SQL cannot orphan or erase history.
   If deletion is ever added, prefer a soft delete (`active` flag) and keep the rows.
2. **What happens if a doctor is deleted?**
   `DELETE /doctor/{id}` must remove the doctor **and their appointments**
   (`deleteAllByDoctorId`). The FK cascades so MySQL guarantees the
   same outcome the service implements, including cascade of their slots.
3. **Should a doctor be allowed overlapping appointments?**
   No. Appointment duration is fixed at 1 hour, so two appointments
   overlap iff they share the same start time → `UNIQUE (doctor_id, appointment_time)`
   blocks it at the DB level, and the service additionally validates the start
   time belongs to a `doctor_available_times` slot before inserting (defense in depth).
4. **Should each doctor have their own available time slots?**
   Yes — stored per doctor in `doctor_available_times` (the `@ElementCollection`
   table). Slots are doctor-owned data that admins edit on doctor create/update;
   embedding them in a JSON column would forfeit FK cascade and per-slot querying.
5. **Should a patient's past appointment history be retained forever?**
   Yes. Appointments are only removed when their doctor is deleted (operational
   cascade); otherwise rows persist with `status` transitions (0 → 1), preserving
   the history the patient/doctor dashboards read.
6. **Is a prescription tied to a specific appointment?**
   Logically yes, physically no: `appointmentId` lives in MongoDB as a plain
   value reference — there is no cross-engine FK. A prescription is created from
   a completed appointment (which flips to `status = 1`) but is stored
   independently in the `prescriptions` collection.
7. **Email/phone format validation: DB or code?**
   In code (Bean Validation → 400 via `ValidationFailed`): consistent error
   messages, portable across engines, and regex-friendly. The DB enforces only
   what it must: `NOT NULL`, `UNIQUE`, and length.

---

## MongoDB Collection Design

Database: `prescriptions`, collection: **`prescriptions`**.
This collection complements the MySQL schema by holding the *document-shaped*
side of the clinic: prescriptions (and, later, similar free-form records such as
feedback or messages) whose fields are expected to evolve without `ALTER TABLE`.

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc123456')",
  "appointmentId": 51,
  "patientName": "John Smith",
  "medication": "Paracetamol",
  "dosage": "500mg",
  "doctorNotes": "Take 1 tablet every 6 hours.",
  "refillCount": 2,
  "tags": ["pain-relief", "otc", "short-course"],
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  },
  "followUp": {
    "recommended": true,
    "inDays": 14
  },
  "metadata": {
    "createdAt": "2026-10-21T10:30:00Z",
    "createdBy": "doctor@clinic.com",
    "source": "doctor-dashboard",
    "schemaVersion": 2
  }
}
```

- `appointmentId` — logical link to the MySQL appointment (no FK across engines);
  unique index on this field for the `GET /prescription/{appointmentId}` lookup.
- `patientName`, `medication`, `dosage`, `doctorNotes` — current application
  contract (required by the `Prescription` `@Document` entity).
- `refillCount`, `tags[]` — arrays for lightweight categorization/filtering;
  optional, so old documents without them remain valid.
- `pharmacy`, `followUp` — embedded documents; one prescription reads as one
  document, no joins needed (MongoDB philosophy: embed what is read together).
- `metadata` — audit/evolution block: `schemaVersion` lets future code migrate
  old documents lazily instead of running a one-shot migration script.

### Design decisions and justifications

1. **Full patient object or just an ID/name?**
   Only the denormalized `patientName` snapshot — enough to display the
   prescription without a cross-database hop. The canonical patient record
   (email, phone, address) stays in MySQL; embedding the whole object would
   create stale duplicates the moment the patient edits their profile.
2. **What would a chat message document look like?**
   If a `messages` collection is added later, the same patterns apply:
   references by value (`appointmentId`, `senderId`), the payload embedded
   (`body`, `attachments[]`), and an audit `metadata` block — e.g.
   `{ "appointmentId": 51, "senderId": 7, "senderRole": "doctor",
   "body": "How is the pain?", "attachments": [], "readBy": [7],
   "metadata": { "createdAt": "...", "schemaVersion": 1 } }`.
   Embedding a whole conversation thread is deliberately avoided (unbounded
   growth); one document per message keeps writes cheap.
3. **What happens if the schema needs to evolve?**
   The design supports it: new fields are simply absent-tolerant on old
   documents (schemaless, no migration), optional-by-contract fields never break
   the current entity, arrays/embedded docs can grow structure without DDL, and
   `metadata.schemaVersion` records which shape each document has.
   Replacing a field's structure (e.g. `dosage: string` → object) would be a
   breaking change and must go through `schemaVersion`-gated code.
