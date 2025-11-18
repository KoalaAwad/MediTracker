import { http } from "./http.js"

export async function getCsrfToken(){
    const res = await fetch("http://localhost:8080/login", {
        credentials: "include"
    });
    const html = await res.text();
    const match = html.match(/name="_csrf" value="([^"]+)"/);
    return match ? match [1]: null;
}

export async function login(username, password){
    const csrf = await getCsrfToken();
    const formData = new URLSearchParams();
    formData.append("username", username);
    formData.append("password", password);
    formData.append("_csrf", csrf);
    
    const res = await fetch("http://localhost:8080/login", {
        method: "POST",
        body: formData,
        credentials: "include"
    });

    if (res.redirected) return true;
        throw new Error("Invalid credentials");

}