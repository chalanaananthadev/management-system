const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export async function api(path, options = {}) {
  let response;

  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {}),
      },
    });
  } catch (error) {
    throw new Error("Cannot reach the backend. Start Spring Boot on port 8080 and try again.");
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let body = null;

  if (text) {
    try {
      body = JSON.parse(text);
    } catch (error) {
      throw new Error(response.ok ? "The backend returned an unexpected response." : `Request failed (${response.status}).`);
    }
  }

  if (!response.ok) {
    throw new Error(body?.message || `Request failed (${response.status}).`);
  }

  return body;
}
