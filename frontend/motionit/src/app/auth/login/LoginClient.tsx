"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { APP_NAME } from "@/constants";
import { authService } from "@/services";
import { useUser } from "../../../stores";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "");
const kakaoAuthEnv = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL?.trim();
const kakaoAuthUrl =
  kakaoAuthEnv && kakaoAuthEnv.length > 0
    ? kakaoAuthEnv
    : apiBaseUrl
      ? `${apiBaseUrl}/oauth2/authorization/kakao`
      : null;

export default function LoginClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const registered = searchParams.get("registered") === "true";

  const { setUser } = useUser();

  const [formValues, setFormValues] = useState({
    email: "",
    password: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (registered) {
      setInfo("회원가입이 완료되었습니다. 로그인해주세요.");
    }
  }, [registered]);

  const handleChange =
    (field: "email" | "password") => (event: React.ChangeEvent<HTMLInputElement>) => {
      setFormValues((prev) => ({
        ...prev,
        [field]: event.target.value,
      }));
    };

  const validate = () => {
    if (!formValues.email.trim()) {
      return "이메일을 입력해주세요.";
    }
    if (!/\S+@\S+\.\S+/.test(formValues.email)) {
      return "올바른 이메일 형식을 입력해주세요.";
    }
    if (!formValues.password.trim()) {
      return "비밀번호를 입력해주세요.";
    }
    return null;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setInfo(null);

    const validationMessage = validate();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await authService.login({
        email: formValues.email.trim(),
        password: formValues.password,
      });

      setUser({
        id: response.id,
        nickname: response.nickname,
        email: response.email,
      });

      router.replace("/rooms");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "로그인에 실패했습니다. 다시 시도해주세요.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleKakaoLogin = () => {
    if (!kakaoAuthUrl) {
      setError("카카오 로그인 경로가 설정되지 않았습니다.");
      return;
    }
    window.location.href = kakaoAuthUrl;
  };

  return (
    <div className="min-h-screen bg-[#f6f9fb] px-4 py-10">
      <div className="mx-auto flex min-h-[calc(100vh-80px)] max-w-md items-center justify-center">
        <div className="w-full rounded-3xl border border-[#e3ebf3] bg-white px-10 py-12 shadow-[0_12px_40px_rgba(15,35,60,0.08)]">
          <div className="text-center">
            <p className="text-5xl font-black tracking-tight text-[#132c4b]">
              {APP_NAME}
            </p>
            <h1 className="mt-6 text-2xl font-semibold text-[#152941]">로그인</h1>
            <p className="mt-2 text-sm text-[#7b8b9c]">계정에 로그인하세요</p>
          </div>

          {info && (
            <div className="mt-8 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {info}
            </div>
          )}

          {error && (
            <div className="mt-8 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
              {error}
            </div>
          )}

          <form className="mt-10 space-y-6" onSubmit={handleSubmit}>
            <div>
              <label className="mb-2 block text-sm font-semibold text-[#1e3147]">
                이메일
              </label>
              <input
                type="email"
                value={formValues.email}
                onChange={handleChange("email")}
                placeholder="이메일을 입력하세요"
                autoComplete="email"
                className="w-full rounded-2xl border border-[#d8e2ec] bg-white px-4 py-3 text-sm text-[#14263d] placeholder:text-[#aab6c2] focus:border-transparent focus:outline-none focus:ring-2 focus:ring-[#0aa37a]"
              />
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-[#1e3147]">
                비밀번호
              </label>
              <input
                type="password"
                value={formValues.password}
                onChange={handleChange("password")}
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
                className="w-full rounded-2xl border border-[#d8e2ec] bg-white px-4 py-3 text-sm text-[#14263d] placeholder:text-[#aab6c2] focus:border-transparent focus:outline-none focus:ring-2 focus:ring-[#0aa37a]"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-2xl bg-[#0aa37a] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#068c68] disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSubmitting ? "로그인 중..." : "로그인"}
            </button>
          </form>

          <div className="mt-10 flex items-center gap-3 text-xs text-[#98a7b6]">
            <span className="h-px flex-1 bg-[#e6eef5]" />
            <span>또는</span>
            <span className="h-px flex-1 bg-[#e6eef5]" />
          </div>

          <button
            type="button"
            onClick={handleKakaoLogin}
            className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl bg-[#fee500] px-4 py-3 text-sm font-semibold text-[#3c1e1e] transition hover:bg-[#f9dd00]"
          >
            <span className="rounded-full bg-black px-2 py-0.5 text-xs font-bold text-[#fee500]">
              카카오
            </span>
            카카오로 계속하기
          </button>

          <div className="mt-10 text-center text-sm text-[#1f6b62]">
            계정이 없으신가요?{" "}
            <Link href="/auth/signup" className="font-semibold hover:underline">
              회원가입
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
