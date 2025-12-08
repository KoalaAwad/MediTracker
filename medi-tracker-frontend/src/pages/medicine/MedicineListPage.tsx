import { useAuth } from "../../context/AuthContext";
import MedicineList from "../../components/medicine/MedicineList";

export default function MedicineListPage() {
  const { user } = useAuth();
  const isAdmin = user?.role.includes("ADMIN") || false;
  const isDoctor = user?.role.includes("DOCTOR") || false;

  return <MedicineList isAdmin={isAdmin} isDoctor={isDoctor} />;
}

