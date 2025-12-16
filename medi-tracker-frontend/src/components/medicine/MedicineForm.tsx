import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { medicineApi, Medicine } from "../../api/medicineApi";
import { useAuthStore } from "../../zustand/authStore";

export default function MedicineForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEditMode = !!id;

  const [formData, setFormData] = useState<Medicine>({
    name: "",
    genericName: "",
    manufacturer: "",
    dosageForm: "",
    strength: "",
    description: "",
    sideEffects: "",
    contraindications: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const token = useAuthStore((s) => s.token);

  useEffect(() => {
    if (isEditMode && id) {
      loadMedicine(parseInt(id));
    }
  }, [id]);

  const loadMedicine = async (medicineId: number) => {
    try {
      if (!token) {
        navigate("/login");
        return;
      }

      const response = await medicineApi.getById(medicineId, token);
      setFormData(response.data);
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to load medicine");
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (!token) {
        navigate("/login");
        return;
      }

      if (isEditMode && id) {
        await medicineApi.update(parseInt(id), formData, token);
      } else {
        await medicineApi.create(formData, token);
      }

      navigate("/medicine");
    } catch (err: any) {
      setError(err.response?.data?.error || "Failed to save medicine");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4">
        <div className="bg-white rounded-lg shadow-lg p-8">
          <h1 className="text-3xl font-bold mb-6">
            {isEditMode ? "Edit Medicine" : "Add Medicine"}
          </h1>

          {error && (
            <div className="bg-red-50 border-l-4 border-red-500 p-4 mb-6">
              <p className="text-red-700">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Medicine Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Generic Name
              </label>
              <input
                type="text"
                value={formData.genericName || ""}
                onChange={(e) => setFormData({ ...formData, genericName: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Manufacturer
              </label>
              <input
                type="text"
                value={formData.manufacturer || ""}
                onChange={(e) => setFormData({ ...formData, manufacturer: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Dosage Form
                </label>
                <input
                  type="text"
                  value={formData.dosageForm || ""}
                  onChange={(e) => setFormData({ ...formData, dosageForm: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., Tablet, Capsule"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Strength
                </label>
                <input
                  type="text"
                  value={formData.strength || ""}
                  onChange={(e) => setFormData({ ...formData, strength: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., 500mg"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                value={formData.description || ""}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={3}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Side Effects
              </label>
              <textarea
                value={formData.sideEffects || ""}
                onChange={(e) => setFormData({ ...formData, sideEffects: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={3}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Contraindications
              </label>
              <textarea
                value={formData.contraindications || ""}
                onChange={(e) =>
                  setFormData({ ...formData, contraindications: e.target.value })
                }
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={3}
              />
            </div>

            <div className="flex gap-4 pt-4">
              <button
                type="submit"
                disabled={loading}
                className="bg-sky-500 hover:bg-sky-700 text-white px-6 py-2 rounded-lg transition-colors disabled:bg-gray-400"
              >
                {loading ? "Saving..." : "Save changes"}
              </button>
              <button
                type="button"
                onClick={() => navigate("/medicine")}
                className="bg-gray-300 hover:bg-gray-400 text-black px-6 py-2 rounded-lg transition-colors"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
