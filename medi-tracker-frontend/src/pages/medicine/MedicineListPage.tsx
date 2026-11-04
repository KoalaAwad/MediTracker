import { useAuthStore } from "../../zustand/authStore";
import MedicineList from "../../components/medicine/MedicineList";

export default function MedicineListPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role.includes("ADMIN") || false;

  return <MedicineList isAdmin={isAdmin} />;
}
