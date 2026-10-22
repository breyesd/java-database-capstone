// doctorServices.js — API communication for doctor-related actions
import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

// Fetch all doctors (admin & patient dashboards)
export async function getDoctors() {
  try {
    const response = await fetch(`${DOCTOR_API}`);
    const data = await response.json();
    return data.doctors;
  } catch (error) {
    console.error("Error fetching doctors:", error);
    return [];
  }
}

// Delete a doctor by id (admin only)
export async function deleteDoctor(id, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${id}/${token}`, {
      method: "DELETE",
    });
    const data = await response.json();
    return {
      success: response.ok,
      message: data.message,
    };
  } catch (error) {
    console.error("Error deleting doctor:", error);
    return {
      success: false,
      message: "Failed to delete doctor",
    };
  }
}

// Save (create) a new doctor (admin only)
export async function saveDoctor(doctor, token) {
  try {
    const response = await fetch(`${DOCTOR_API}/${token}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(doctor),
    });
    const data = await response.json();
    return {
      success: response.ok,
      message: data.message,
    };
  } catch (error) {
    console.error("Error saving doctor:", error);
    return {
      success: false,
      message: "Failed to save doctor",
    };
  }
}

// Filter doctors by name, time and specialty ("null" string = no filter)
export async function filterDoctors(name, time, specialty) {
  try {
    const response = await fetch(
      `${DOCTOR_API}/filter/${name}/${time}/${specialty}`
    );
    if (response.ok) {
      return await response.json();
    }
    console.error("Failed to filter doctors:", response.statusText);
    return { doctors: [] };
  } catch (error) {
    console.error("Error filtering doctors:", error);
    alert("❌ An error occurred while filtering doctors.");
    return { doctors: [] };
  }
}
