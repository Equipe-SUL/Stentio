import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ScrollView, Pressable, ActivityIndicator } from 'react-native';
import Svg, { Defs, Pattern, Rect, Circle, G } from 'react-native-svg';
import Animated, { useSharedValue, useAnimatedStyle, withRepeat, withTiming, Easing, withDelay } from 'react-native-reanimated';
import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { api, getApiErrorMessage } from '../lib/api';
import { setToken } from '../lib/auth';

type Cargo = "Admin" | "Atendente" | "Financeiro" | "Gestor_Projeto";

interface LoginResponse {
  token: string;
  tipo: string;
  expiresIn: number;
  email: string;
  role: Cargo;
}

const languagesListA = [
  "Hallo, Willkommen", "مرحبا بك", "你好，歡迎", "안녕하세요, 환영합니다",
  "Zdravo, dobrodošli", "Hej, velkommen", "Hola, bienvenido", "Bonjour, bienvenue",
  "Γεια, καλώς ήρθατε", "שלום, ברוך הבא", "नमस्ते, स्वागत है", "Hallo, welkom",
  "Szia, üdvözlünk", "Hello, Welcome", "Ciao, benvenuto", "こんにちは、ようこそ"
];

const languagesListB = [
  "Salve, grata", "Сайн байна уу, тавтай морил", "Hei, velkommen", "سلام، خوش آمدید",
  "Cześć, witaj", "Здравствуйте, добро пожаловать", "Здраво, добродошли",
  "Salaam, soo dhawow", "Hej, välkommen", "สวัสดี ยินดีต้อนรับ", "Ahoj, vítejte",
  "Merhaba, hoş geldiniz", "Привіт, ласкаво просимо", "Kalunga, ondonge", "Xin chào, chào mừng", "你好，欢迎"
];

const generateCycle = (langArray: string[]) => {
  const words = [];
  let langIdx = 0;
  for (let i = 0; i < 30; i++) {
    if (i % 4 === 0) {
      words.push("Olá, Bem-vindo"); 
    } else {
      words.push(langArray[langIdx % langArray.length]);
      langIdx++;
    }
  }
  return words;
};

const spiralWordsA = generateCycle(languagesListA);
const spiralWordsB = generateCycle(languagesListB);

const INTERVAL = 1800; 
const VISIBLE_LIFESPAN = 9000; 
const TOTAL_CYCLE = spiralWordsA.length * INTERVAL; 
const VISIBLE_FRACTION = VISIBLE_LIFESPAN / TOTAL_CYCLE; 

const AnimatedSpiralWord = ({ word, index, angleOffset, cycleLength }: { word: string, index: number, angleOffset: number, cycleLength: number }) => {
  const progress = useSharedValue(0);

  useEffect(() => {
    const randomOffset = (index * 41) % 400; 
    const delay = (index * INTERVAL) + randomOffset;
    const currentTotalCycle = cycleLength * INTERVAL;

    progress.value = withDelay(
      delay,
      withRepeat(withTiming(1, { duration: currentTotalCycle, easing: Easing.linear }), -1, false)
    );
  }, []);

  const animatedStyle = useAnimatedStyle(() => {
    if (progress.value === 0 || progress.value > VISIBLE_FRACTION) {
      return { opacity: 0, position: 'absolute' };
    }

    const p = progress.value / VISIBLE_FRACTION; 
    const radius = 380 * p; 
    const angle = (p * Math.PI * 3.5) + (index * 1.8) + angleOffset;
    
    const x = Math.cos(angle) * radius;
    const y = Math.sin(angle) * radius;

    const maxScale = index % 4 === 0 ? 2.8 : index % 4 === 1 ? 2.0 : index % 4 === 2 ? 1.5 : 1.1;
    const currentScale = maxScale * (0.4 + (p * 0.6));

    let opacityMultiplier = 0;
    if (p < 0.15) opacityMultiplier = p / 0.15; 
    else if (p > 0.8) opacityMultiplier = (1 - p) / 0.2; 
    else opacityMultiplier = 1; 

    return {
      position: 'absolute',
      transform: [{ translateX: x }, { translateY: y }, { scale: currentScale }],
      opacity: opacityMultiplier * 0.75, 
    };
  });

  return (
    <Animated.Text 
      style={[animatedStyle, { color: 'rgba(255, 255, 255, 0.55)' }]} 
      className="font-serif font-medium whitespace-nowrap"
    >
      {word}
    </Animated.Text>
  );
};

const BrandingCarousel = ({ textColor = "text-white", dotActiveColor = "#ffffff", dotInactiveColor = "rgba(255,255,255,0.4)" }: { textColor?: string, dotActiveColor?: string, dotInactiveColor?: string }) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const slides = [
    "Traduza com precisão,\ncontrole com clareza.",
    "Cada palavra no\nlugar certo.",
    "60 idiomas,\num único painel."
  ];

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % slides.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [currentIndex]);

  return (
    <View className="z-10 w-full">
      <View className="h-[90px] justify-end mb-5 relative">
        {slides.map((slide, index) => {
          const isActive = index === currentIndex;
          const opacity = useSharedValue(isActive ? 1 : 0);
          const translateY = useSharedValue(isActive ? 0 : 10);

          useEffect(() => {
            opacity.value = withTiming(isActive ? 1 : 0, { duration: 600 });
            translateY.value = withTiming(isActive ? 0 : 10, { duration: 600 });
          }, [isActive]);

          const style = useAnimatedStyle(() => ({
            opacity: opacity.value,
            transform: [{ translateY: translateY.value }],
            position: 'absolute',
            bottom: 0,
            left: 0,
          }));

          return (
            <Animated.Text 
              key={index} 
              style={style} 
              className={`${textColor} text-3xl md:text-4xl font-serif font-bold leading-tight`}
            >
              {slide}
            </Animated.Text>
          );
        })}
      </View>

      <View className="flex-row gap-3 items-center h-4">
        {slides.map((_, index) => {
          const isActive = index === currentIndex;
          const width = useSharedValue(isActive ? 32 : 10);
          const opacity = useSharedValue(isActive ? 1 : 0.4);

          useEffect(() => {
            width.value = withTiming(isActive ? 32 : 10, { duration: 400 });
            opacity.value = withTiming(isActive ? 1 : 0.4, { duration: 400 });
          }, [isActive]);

          const dotStyle = useAnimatedStyle(() => ({
            width: width.value,
            opacity: opacity.value,
            height: 10,
            borderRadius: 9999,
          }));

          return (
            <TouchableOpacity 
              key={index}
              onPress={() => setCurrentIndex(index)}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
              activeOpacity={0.8}
            >
              <Animated.View style={[dotStyle, { backgroundColor: isActive ? dotActiveColor : dotInactiveColor }]} />
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
};

const LeftArtPanel = () => {
  return (
    <View className="flex-1 bg-[#8c5230] overflow-hidden justify-between p-12">
      <View className="absolute inset-0 items-center justify-center pointer-events-none">
        <Svg height="100%" width="100%" style={{ position: 'absolute' }}>
          <Defs>
            <Pattern id="grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <Rect width="40" height="40" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="1" />
            </Pattern>
          </Defs>
          <Rect width="100%" height="100%" fill="url(#grid)" />
          
          <G stroke="rgba(255,255,255,0.06)" strokeWidth="1" fill="none">
            <Circle cx="50%" cy="50%" r="120" />
            <Circle cx="50%" cy="50%" r="200" />
          </G>
        </Svg>

        {spiralWordsA.map((word, index) => (
          <AnimatedSpiralWord 
            key={`a-${index}`} 
            word={word} 
            index={index} 
            angleOffset={0} 
            cycleLength={spiralWordsA.length}
          />
        ))}

        {spiralWordsB.map((word, index) => (
          <AnimatedSpiralWord 
            key={`b-${index}`} 
            word={word} 
            index={index} 
            angleOffset={2.1} 
            cycleLength={spiralWordsB.length}
          />
        ))}
      </View>

      <View className="flex-row justify-between items-center w-full z-10">
        <Text className="text-white font-bold text-2xl tracking-widest">STENTIO</Text>
      </View>

      <BrandingCarousel textColor="text-white" dotActiveColor="#ffffff" dotInactiveColor="rgba(255,255,255,0.4)" />
    </View>
  );
};

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberSession, setRememberSession] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async () => {
    setErrorMessage('');
    if (!email || !password) {
      setErrorMessage('Por favor, preencha o e-mail e a senha.');
      return;
    }

    setIsLoading(true);
    try {
      const { data } = await api.post<LoginResponse>('/api/v1/usuarios/login', {
        email,
        senha: password,
      });

      await setToken(data.token, rememberSession);
      router.replace('/usuarios');
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View className="flex-1 bg-white">
      <ScrollView contentContainerStyle={{ flexGrow: 1 }} className="flex-1">
        <View className="flex-1 flex-col md:flex-row w-full min-h-screen">
          
          <View className="hidden md:flex flex-1">
            <LeftArtPanel />
          </View>

          <View className="flex-1 items-center justify-center p-6 md:p-12 w-full z-10 bg-white">
            <View className="w-full max-w-md">
              
              <View className="md:hidden items-center mb-6">
                <Text className="text-[#8c5230] font-bold text-3xl tracking-widest">STENTIO</Text>
              </View>

              <Text className="text-3xl md:text-4xl font-serif font-bold text-zinc-900 mb-6">
                Entrar na conta
              </Text>

              {errorMessage ? (
                <View className="w-full bg-red-50 border border-red-200 rounded-xl p-3 mb-5">
                  <Text className="text-red-600 text-sm font-medium text-center">{errorMessage}</Text>
                </View>
              ) : null}

              <Text className="text-zinc-700 font-semibold mb-2 ml-1">E-mail</Text>
              <TextInput 
                placeholder="voce@empresa.com"
                placeholderTextColor="#a1a1aa"
                keyboardType="email-address"
                autoCapitalize="none"
                value={email}
                onChangeText={setEmail}
                className="w-full border border-zinc-200 rounded-2xl px-5 py-4 bg-zinc-50 text-zinc-900 text-base mb-5"
              />

              <View className="flex-row justify-between items-center mb-2 ml-1">
                <Text className="text-zinc-700 font-semibold">Senha</Text>
              </View>

              <View className="w-full flex-row items-center border border-zinc-200 rounded-2xl bg-zinc-50 px-5 mb-6">
                <TextInput 
                  placeholder="••••••••"
                  placeholderTextColor="#a1a1aa"
                  secureTextEntry={!showPassword}
                  value={password}
                  onChangeText={setPassword}
                  className="flex-1 py-4 text-zinc-900 text-base"
                />
                <TouchableOpacity 
                  onPress={() => setShowPassword(!showPassword)}
                  className="p-2"
                >
                  <Ionicons 
                    name={showPassword ? "eye-off-outline" : "eye-outline"} 
                    size={22} 
                    color="#8c5230" 
                  />
                </TouchableOpacity>
              </View>

              <Pressable 
                onPress={() => setRememberSession(!rememberSession)}
                className="flex-row items-center mb-4"
              >
                <View className={`w-5 h-5 border-2 rounded-md mr-3 items-center justify-center ${rememberSession ? 'bg-[#8c5230] border-[#8c5230]' : 'border-zinc-300 bg-white'}`}>
                  {rememberSession && <Ionicons name="checkmark" size={14} color="white" />}
                </View>
                <Text className="text-zinc-600 text-sm font-medium">Salvar sessão</Text>
              </Pressable>

              <Pressable 
                onPress={() => setAgreeTerms(!agreeTerms)}
                className="flex-row items-center mb-8"
              >
                <View className={`w-5 h-5 border-2 rounded-md mr-3 items-center justify-center ${agreeTerms ? 'bg-[#8c5230] border-[#8c5230]' : 'border-zinc-300 bg-white'}`}>
                  {agreeTerms && <Ionicons name="checkmark" size={14} color="white" />}
                </View>
                <Text className="text-zinc-600 text-sm">
                  Concordo com os{' '}
                  <Text className="text-[#8c5230] underline font-semibold">Termos e Condições</Text>
                </Text>
              </Pressable>

              <TouchableOpacity 
                onPress={handleLogin}
                disabled={isLoading}
                className="w-full bg-[#8c5230] py-4 rounded-2xl items-center shadow-md shadow-orange-900/20 active:opacity-80"
              >
                {isLoading ? (
                  <ActivityIndicator color="#fff" />
                ) : (
                  <Text className="text-white font-bold text-lg tracking-wide">Entrar</Text>
                )}
              </TouchableOpacity>

            </View>

            <View className="md:hidden w-full bg-white px-6 pt-12 pb-8 mt-12 border-t border-zinc-100 items-center">
              <View className="w-full max-w-md">
                <BrandingCarousel 
                  textColor="text-[#8c5230]" 
                  dotActiveColor="#8c5230" 
                  dotInactiveColor="#d1b2a3" 
                />
              </View>
            </View>

          </View>

        </View>
      </ScrollView>
    </View>
  );
}