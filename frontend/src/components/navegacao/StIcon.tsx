import Svg, { Circle } from "react-native-svg";

interface StIconProps {
  size: number;
  color: string;
}

export function StIcon({ size, color }: StIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 1600 1304">
      <Circle cx={283} cy={652} r={225} fill={color} />
      <Circle cx={770} cy={502} r={98} fill={color} />
      <Circle cx={770} cy={802} r={98} fill={color} />
      <Circle cx={1125} cy={325} r={80} fill={color} />
      <Circle cx={1125} cy={652} r={80} fill={color} />
      <Circle cx={1125} cy={978} r={80} fill={color} />
      <Circle cx={1480} cy={118} r={60} fill={color} />
      <Circle cx={1480} cy={475} r={60} fill={color} />
      <Circle cx={1480} cy={830} r={60} fill={color} />
      <Circle cx={1480} cy={1185} r={60} fill={color} />
    </Svg>
  );
}
