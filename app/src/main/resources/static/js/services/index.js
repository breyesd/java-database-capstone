// services/index.js — role-selection page login wiring (admin & doctor)
import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

const ADMIN_API = API_BASE_URL + "/admin";
const DOCTOR_API = API_BASE_URL + "/doctor/login";

window.onload = () => {
  const adminLoginBtn = document.getElementById("adminLogin");
  if (adminLoginBtn) {
    adminLoginBtn.addEventListener("click", () => openModal("adminLogin"));
  }

  const doctorLoginBtn = document.getElementById("doctorLogin");
  if (doctorLoginBtn) {
    doctorLoginBtn.addEventListener("click", () => openModal("doctorLogin"));
  }
};

window.adminLoginHandler = async function () {
  try {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const admin = { username, password };

    const response = await fetch(ADMIN_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(admin),
    });

    if (response.ok) {
      const result = await response.json();
      localStorage.setItem("token", result.token);
      selectRole("admin");
    } else {
      alert("❌ Invalid credentials!");
    }
  } catch (error) {
    console.error("Admin login failed:", error);
    alert("❌ An error occurred during admin login.");
  }
};

window.doctorLoginHandler = async function () {
  try {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const doctor = { email, password };

    const response = await fetch(DOCTOR_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });

    if (response.ok) {
      const result = await response.json();
      localStorage.setItem("token", result.token);
      selectRole("doctor");
    } else {
      alert("❌ Invalid credentials!");
    }
  } catch (error) {
    console.error("Doctor login failed:", error);
    alert("❌ An error occurred during doctor login.");
  }
};
