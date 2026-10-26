// SelectField.tsx
import { StyleSheet, Text, View } from "react-native";
import { Picker } from "@react-native-picker/picker";
import type { OpcaoSelect } from "./types";

// O Picker é um componente de terceiros e não recebe className.
const styles = StyleSheet.create({
  picker: { color: "#171717" },
});

interface SelectFieldProps {
  label: string;
  value: string;
  onChange: (valor: string) => void;
  options: OpcaoSelect[];
  placeholder?: string;
  error?: string;
  // Borda vermelha sem mensagem própria (ex.: o par de idiomas iguais).
  invalid?: boolean;
  disabled?: boolean;
}

export function SelectField({
  label,
  value,
  onChange,
  options,
  placeholder = "Selecione...",
  error,
  invalid = false,
  disabled = false,
}: SelectFieldProps) {
  const comErro = Boolean(error) || invalid;

  return (
    <View className="flex-1 gap-2">
      <Text className="text-sm font-medium text-neutral-700">{label}</Text>

      <View
        className={`overflow-hidden rounded-lg border bg-white ${
          comErro ? "border-red-400" : "border-neutral-300"
        } ${disabled ? "opacity-60" : ""}`}
      >
        <Picker
          selectedValue={value}
          onValueChange={onChange}
          enabled={!disabled}
          dropdownIconColor="#737373"
          style={styles.picker}
          accessibilityLabel={label}
        >
          <Picker.Item label={placeholder} value="" />
          {options.map((opcao) => (
            <Picker.Item key={opcao.value} label={opcao.label} value={opcao.value} />
          ))}
        </Picker>
      </View>

      {error ? <Text className="text-xs font-medium text-red-600">{error}</Text> : null}
    </View>
  );
}