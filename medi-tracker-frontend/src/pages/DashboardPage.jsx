import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";


export default function DashboardPage(){
    const {user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate("/");
    };

    if (!user){
        return <p> Loading or not logged in ...</p>;
    }
    return(
        <div className="dashboard-page">
            <h1>Welcome, {user.name}! </h1>
            <button onClick={handleLogout}>Logout</button>
        </div>
    );

}

