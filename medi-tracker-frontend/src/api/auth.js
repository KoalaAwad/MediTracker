export async function getCsrfToken() {
  const res = await fetch("http://localhost:8080/login", {
    credentials: "include",
  });
  const html = await res.text();
  const match = html.match(/name="_csrf" value="([^"]+)"/);
  return match ? match[1] : null;
}

export async function login(email, password) {
  const res = await fetch("http://localhost:8080/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify({
      email: email,
      password: password,
    }),
  });

  if (!res.ok) {
    const error = await res.json();
    throw new Error(error.error || "Login failed");
  }
  const data = await res.json();
  return data;
}
