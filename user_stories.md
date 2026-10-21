# User Stories — Smart Clinic Management System

## Admin User Stories

**Title:**
_As an admin, I want to log in with my username and password, so that I can access the admin dashboard securely._

**Acceptance Criteria:**
1. `POST /admin` with a valid `Admin` body (username + password) returns 200 with a JWT token.
2. Invalid credentials or unknown username return 401 with an error message; unexpected errors return 500.
3. The token opens the admin dashboard (`GET /adminDashboard/{token}`); an invalid token redirects to `/`.

**Priority:** High
**Story Points:** 3
**Notes:**
- The token subject is the admin username and expires after 7 days.
- Passwords are never exposed in JSON responses (WRITE_ONLY).

---

**Title:**
_As an admin, I want to register a new doctor, so that doctors can be added to the clinic and appear for patients._

**Acceptance Criteria:**
1. `POST /doctor/{token}` accepts a validated `Doctor` body only when the token belongs to an admin role.
2. A successful registration returns 200 with a success message; a duplicate doctor returns 409; an invalid/foreign token returns 401.
3. Field validation is enforced: name 3–100 chars, specialty 3–50, valid email, password ≥6, phone exactly 10 digits, at least the available time slots provided.

**Priority:** High
**Story Points:** 5
**Notes:**
- `availableTimes` uses the format `"HH:mm-HH:mm"`; it defines the bookable slots for appointments.

---

**Title:**
_As an admin, I want to update a doctor's information, so that their profile and availability stay current._

**Acceptance Criteria:**
1. `PUT /doctor/{token}` updates the doctor when the token belongs to an admin and the doctor exists (200).
2. A non-existent doctor returns 404; an invalid token returns 401.
3. Updated fields are re-validated with the same rules as registration.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Changing `availableTimes` affects which appointment slots patients can book afterward.

---

**Title:**
_As an admin, I want to remove a doctor from the system, so that doctors who leave the clinic no longer receive appointments._

**Acceptance Criteria:**
1. `DELETE /doctor/{id}/{token}` deletes the doctor when the token belongs to an admin and the doctor exists (200).
2. All appointments linked to that doctor are deleted as well (`deleteAllByDoctorId`).
3. Unknown doctor id returns 404; invalid token returns 401.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Cascading deletion is transactional (`@Modifying @Transactional`).

---

**Title:**
_As an admin, I want to view the full list of doctors, so that I can manage the clinic's staff from my dashboard._

**Acceptance Criteria:**
1. `GET /doctor` returns 200 with the complete list under the `doctors` key.
2. Passwords are excluded from the response.
3. The admin dashboard renders the list without requiring additional filters.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- The same endpoint backs the patient-facing doctor browser.

---

## Patient User Stories

**Title:**
_As a patient, I want to register with my personal data, so that I can create my account in the system._

**Acceptance Criteria:**
1. `POST /patient` creates a patient when the email and phone are not already registered (success message, 200).
2. A duplicate email or phone number is rejected with an error message.
3. Validation is enforced: name 3–100, valid email, password ≥6, phone exactly 10 digits, address ≤255 chars.

**Priority:** High
**Story Points:** 3
**Notes:**
- Uniqueness check runs before persisting (`validatePatient`).

---

**Title:**
_As a patient, I want to log in with my email and password, so that I can access my personal dashboard._

**Acceptance Criteria:**
1. `POST /patient/login` with valid credentials returns 200 with a JWT token.
2. Wrong password or unknown email returns 401; unexpected errors return 500.
3. The token is stored client-side and used as a path variable in subsequent requests.

**Priority:** High
**Story Points:** 3
**Notes:**
- Token subject = patient email; expiry 7 days.

---

**Title:**
_As a patient, I want to book an appointment with a doctor, so that I can receive medical care at a convenient time._

**Acceptance Criteria:**
1. `POST /appointments/{token}` (patient token) creates an appointment with `status = 0` when the requested time matches the start of an available slot of the doctor's `availableTimes`.
2. A non-existent doctor is rejected (−1 case) and an invalid/taken slot is rejected (0 case); valid booking returns success.
3. The appointment time must be in the future (`@Future`), and both doctor and patient are mandatory on the appointment.

**Priority:** High
**Story Points:** 5
**Notes:**
- Appointments have a fixed 1-hour duration (`endTime = appointmentTime + 1h`).
- The doctor list and filters used to choose a doctor come from `GET /doctor` and `GET /doctor/filter/{name}/{time}/{speciality}`.

---

**Title:**
_As a patient, I want to update one of my appointments, so that I can reschedule or correct my booking details._

**Acceptance Criteria:**
1. `PUT /appointments/{token}` updates the appointment when the token belongs to a patient.
2. Success returns 200 with a message; failure (missing appointment or invalid data) returns an appropriate error message.
3. The same availability and future-time rules apply to the new time.

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Only appointments belonging to the authenticated patient may be modified.

---

**Title:**
_As a patient, I want to cancel an appointment, so that I can free up a slot I no longer need._

**Acceptance Criteria:**
1. `DELETE /appointments/{id}/{token}` cancels the appointment when the token belongs to a patient (200 with message).
2. An unknown appointment id returns an error message; an invalid token returns 401.
3. The cancelled appointment no longer appears in the patient's history.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Cancellation semantics (remove vs. status change) follow the service implementation; status 0/1 remains the state model.

---

**Title:**
_As a patient, I want to view my appointment history, so that I can review my past and upcoming visits._

**Acceptance Criteria:**
1. `GET /patient/{id}/{user}/{token}` returns the appointments of that patient when the token is valid for the given `user` role.
2. An invalid token returns 401.
3. Results include doctor name, time, status, and derived fields (date, time-only, end time).

**Priority:** Medium
**Story Points:** 3
**Notes:**
- The same endpoint serves the doctor's view of a patient record (`user = doctor`).

---

**Title:**
_As a patient, I want to filter my appointments by condition or doctor name, so that I can find specific visits quickly._

**Acceptance Criteria:**
1. `GET /patient/filter/{condition}/{name}/{token}` filters by condition and/or doctor name when the token is valid for the patient role.
2. With no filters provided, all of the patient's appointments are returned.
3. Filtering extracts the patient's email from the token — patients cannot access anyone else's data.

**Priority:** Low
**Story Points:** 2
**Notes:**
- Case-insensitive matching on doctor name.

---

**Title:**
_As a patient, I want to view my profile data, so that I can verify my personal information is correct._

**Acceptance Criteria:**
1. `GET /patient/{token}` returns the authenticated patient's data with a valid patient token (200).
2. An invalid token returns 401.
3. The password is never included in the response.

**Priority:** Low
**Story Points:** 2
**Notes:**
- NA

---

## Doctor User Stories

**Title:**
_As a doctor, I want to log in with my email and password, so that I can access my dashboard._

**Acceptance Criteria:**
1. `POST /doctor` with a valid `Login` body returns 200 with a JWT token.
2. Wrong password or unknown email returns 401; unexpected errors return 500.
3. The token opens the doctor dashboard (`GET /doctorDashboard/{token}`); an invalid token redirects to `/`.

**Priority:** High
**Story Points:** 3
**Notes:**
- Token subject = doctor email; expiry 7 days; role validated against the doctor repository.

---

**Title:**
_As a doctor, I want to list my patients' appointments for a given date, so that I can prepare for my workday._

**Acceptance Criteria:**
1. `GET /appointments/{date}/{patientName}/{token}` (doctor token) returns the appointments of that patient on that date.
2. An invalid/expired token returns 401 before any data is read.
3. Each appointment includes patient details and derived fields (date, time, end time).

**Priority:** High
**Story Points:** 3
**Notes:**
- Backed by `findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween`.

---

**Title:**
_As a doctor, I want to issue a prescription for an appointment, so that the patient's treatment is recorded._

**Acceptance Criteria:**
1. `POST /prescription/{token}` (doctor token) saves the prescription in MongoDB with patient name, appointment id, medication, dosage, and optional notes (≤200 chars).
2. On success, the associated appointment status changes to 1 (completed) in MySQL.
3. An invalid token returns 401; missing required fields return 400 via `ValidationFailed`.

**Priority:** High
**Story Points:** 5
**Notes:**
- `appointmentId` is a logical reference to MySQL (no cross-database FK).
- The prescription must reference an existing appointment.

---

**Title:**
_As a doctor, I want to retrieve the prescription of an appointment, so that I can review what was prescribed._

**Acceptance Criteria:**
1. `GET /prescription/{appointmentId}/{token}` (doctor token) returns the prescription document for that appointment (200).
2. An invalid token returns 401; a missing prescription returns an error message with an appropriate status.
3. The response never includes data of unrelated appointments.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Stored in the `prescriptions` MongoDB collection.

---

**Title:**
_As a doctor, I want to view a patient's record and appointment history, so that I can make informed clinical decisions._

**Acceptance Criteria:**
1. `GET /patient/{id}/{user}/{token}` with `user = doctor` and a valid doctor token returns the patient's appointments/record.
2. An invalid token returns 401.
3. The response includes patient contact data and appointment details (name, email, phone, address, status, times).

**Priority:** Medium
**Story Points:** 3
**Notes:**
- Shared endpoint with the patient view; the `user` path variable selects the role context.

---

**Title:**
_As a doctor, I want to check a doctor's availability for a date, so that I know which slots can still be booked._

**Acceptance Criteria:**
1. `GET /doctor/{user}/{doctorId}/{date}/{token}` validates the token against the given `user` type before answering.
2. With a valid token, it returns the availability of the requested doctor for that date (200).
3. An invalid token returns 401 without exposing availability data.

**Priority:** Medium
**Story Points:** 2
**Notes:**
- Availability derives from the doctor's `availableTimes` slots versus already booked appointments.
