// adminDashboard.js — manage doctors from the admin dashboard
import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// "Add Doctor" button in the header opens the modal form
document.getElementById("addDocBtn").addEventListener("click", () => {
  openModal("addDoctor");
});

// Load all doctor cards when the page is ready
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

function loadDoctorCards() {
  getDoctors()
    .then((doctors) => {
      const contentDiv = document.getElementById("content");
      contentDiv.innerHTML = "";

      doctors.forEach((doctor) => {
        const card = createDoctorCard(doctor);
        contentDiv.appendChild(card);
      });
    })
    .catch((error) => {
      console.error("Failed to load doctors:", error);
    });
}

// Re-filter on any search/filter change
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar").value.trim();
  const filterTime = document.getElementById("filterTime").value;
  const filterSpecialty = document.getElementById("filterSpecialty").value;

  const name = searchBar.length > 0 ? searchBar : null;
  const time = filterTime.length > 0 ? filterTime : null;
  const specialty = filterSpecialty.length > 0 ? filterSpecialty : null;

  filterDoctors(name, time, specialty)
    .then((response) => {
      const doctors = response.doctors;
      if (doctors.length > 0) {
        renderDoctorCards(doctors);
      } else {
        document.getElementById("content").innerHTML =
          "<p>No doctors found with the given filters.</p>";
      }
    })
    .catch((error) => {
      console.error("Failed to filter doctors:", error);
      alert("❌ An error occurred while filtering doctors.");
    });
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  doctors.forEach((doctor) => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

// Modal form submit handler (wired by modals.js as adminAddDoctor)
window.adminAddDoctor = async function () {
  try {
    const name = document.getElementById("doctorName").value;
    const specialty = document.getElementById("specialization").value;
    const email = document.getElementById("doctorEmail").value;
    const password = document.getElementById("doctorPassword").value;
    const phone = document.getElementById("doctorPhone").value;
    const availableTimes = Array.from(
      document.querySelectorAll('input[name="availability"]:checked')
    ).map((checkbox) => checkbox.value);

    const token = localStorage.getItem("token");
    if (!token) {
      alert("Session expired. Please log in again.");
      window.location.href = "/";
      return;
    }

    const doctor = { name, specialty, email, password, phone, availableTimes };
    const { success, message } = await saveDoctor(doctor, token);

    if (success) {
      alert(message);
      document.getElementById("modal").style.display = "none";
      loadDoctorCards();
    } else {
      alert(message || "Failed to add doctor.");
    }
  } catch (error) {
    console.error("Add doctor failed:", error);
    alert("❌ An error occurred while adding the doctor.");
  }
};
