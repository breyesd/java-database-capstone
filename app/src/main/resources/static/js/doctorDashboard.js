// doctorDashboard.js — view and filter patient appointments
import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
let selectedDate = new Date().toISOString().split("T")[0];
const token = localStorage.getItem("token");
let patientName = null;

// Search by patient name (empty input → "null" as expected by the backend)
document.getElementById("searchBar").addEventListener("input", () => {
  const value = document.getElementById("searchBar").value.trim();
  patientName = value.length > 0 ? value : null;
  loadAppointments();
});

// "Today's Appointments" button resets to today's date
document.getElementById("todayButton").addEventListener("click", () => {
  selectedDate = new Date().toISOString().split("T")[0];
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});

// Date picker selects another date
document.getElementById("datePicker").addEventListener("change", (e) => {
  selectedDate = e.target.value;
  loadAppointments();
});

// Fetch appointments for the selected date/name and render table rows
async function loadAppointments() {
  try {
    const data = await getAllAppointments(selectedDate, patientName, token);
    const appointments = Array.isArray(data) ? data : (data?.appointments || []);
    tableBody.innerHTML = "";

    if (appointments.length === 0) {
      tableBody.innerHTML =
        '<tr><td colspan="5" class="noPatientRecord">No Appointments found for today.</td></tr>';
      return;
    }    
    appointments.forEach((appointment) => {      
      const row = createPatientRow(appointment.patient, appointment.id, appointment.doctor.id);
      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Failed to load appointments:", error);
    tableBody.innerHTML =
      '<tr><td colspan="5" class="noPatientRecord">Error loading appointments. Try again later.</td></tr>';
  }
}

// Initial render: today's appointments
document.addEventListener("DOMContentLoaded", () => {
  renderContent();
  document.getElementById("datePicker").value = selectedDate;
  loadAppointments();
});
