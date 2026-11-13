import { useLocalSearchParams } from "expo-router";
import { WorkflowScreen } from "../../../../components/ordens/workflow/WorkflowScreen";

export default function OrdemWorkflowRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const idString = Array.isArray(id) ? id[0] : id ?? "";

  return <WorkflowScreen ordemId={idString} />;
}
