# Schema Architecture — Smart Clinic Management System

## Section 1: Architecture summary

The Smart Clinic Management System is a three-tier web application built on Spring Boot. The presentation tier combines two interaction styles: server-rendered Thymeleaf dashboards for admins and doctors (AdminDashboard and DoctorDashboard), and lightweight REST API modules (Appointments, PatientDashboard, PatientRecord) consumed by static JavaScript pages and potential external clients. Both styles converge on the same Spring Boot application tier, which is organized into controllers (Thymeleaf and REST), a shared service layer holding business rules and JWT-based authentication, and a repository layer for data access.

The data tier is deliberately dual-database: MySQL stores the structured relational core (Patient, Doctor, Appointment, Admin) through Spring Data JPA entities, while MongoDB stores flexible document-based prescriptions through Spring Data MongoDB. This separation leverages normalized constraints for transactional entities and schema flexibility for medical records, while keeping each tier independently maintainable, testable, and scalable.

## Section 2: Numbered flow of data and control

1. **User Interface Layer** — Users interact with the system through two kinds of clients: the Thymeleaf-based dashboards (AdminDashboard, DoctorDashboard), which request server-rendered HTML pages, and the REST modules (Appointments, PatientDashboard, PatientRecord), which call the backend's JSON API over HTTP.
2. **Controller Layer** — Each request is routed by URL path and HTTP method to either a Thymeleaf controller (which returns a view name) or a REST controller (which validates input and returns a JSON response). Controllers are the entry points into the backend and enforce request validation before delegating.
3. **Service Layer** — All controllers call the shared service layer, which applies business rules and validations (for example, checking doctor availability before scheduling an appointment) and coordinates workflows across multiple entities, keeping controllers thin and data access isolated.
4. **Repository Layer** — The service layer uses two kinds of repositories to perform data operations: MySQL repositories (Spring Data JPA) for structured relational data, and a MongoDB repository (Spring Data MongoDB) for document-based records.
5. **Database access** — Each repository talks directly to its engine: MySQL stores users, roles, and appointments with a normalized schema and constraints, while MongoDB stores flexible, nested prescription documents that can evolve rapidly.
6. **Model binding** — Rows and documents retrieved from the databases are mapped into Java model classes: MySQL rows become JPA `@Entity` objects, and MongoDB documents become `@Document` objects, giving the application a consistent object-oriented view of the data.
7. **Application models in use** — The bound models are delivered back through the response layer: in MVC flows they are passed to Thymeleaf templates and rendered as HTML, and in REST flows they (or their DTOs) are serialized as JSON in the HTTP response, completing the request–response cycle.
