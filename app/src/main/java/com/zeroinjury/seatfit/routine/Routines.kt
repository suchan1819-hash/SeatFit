package com.zeroinjury.seatfit.routine

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.zeroinjury.seatfit.R

/** 운전석에 앉은 채로 할 수 있는 동작. 10년차 트레이너가 좌석 안 동작으로 설계. */
data class Move(
    val name: String,
    val sub: String,              // 목록에 보이는 짧은 설명
    val cue: String,              // 진행 화면의 핵심 안내 (손·발 위치 → 움직임)
    val voice: String,            // 코치 음성 자막
    val reps: String,             // 횟수 · 시간
    val points: List<String>,     // 자세 포인트
    @DrawableRes val image: Int,       // 동작 자세
    @DrawableRes val startImage: Int,  // 시작 자세
    @RawRes val voiceClip: Int,        // 녹음된 코치 음성 (트레이너 본인 목소리)
    val switchSides: Boolean = false,  // 중간에 반대쪽으로 바꾸는 동작
    val seconds: Int = 60,
)

/** 몸 부위. 카드에 보이는 일러스트와, 그 안에서 확대할 지점(focusX/Y, 0~1). */
enum class BodyPart(
    val label: String,
    val sub: String,
    @DrawableRes val cardImage: Int,
    val focusX: Float,
    val focusY: Float,
) {
    LOWER_BACK("허리", "허리 하부 뻐근함", R.drawable.move_00, 0.36f, 0.50f),
    NECK("목·어깨", "거북목 · 굽은 어깨", R.drawable.move_03, 0.38f, 0.14f),
    HIP("고관절", "오래 앉아 굳은 골반", R.drawable.move_06, 0.40f, 0.62f),
    SHIN_ANKLE("정강이·발목", "엑셀 · 브레이크 피로", R.drawable.move_10, 0.74f, 0.80f),
    WRIST("손목·전완", "핸들 · 스마트폰 피로", R.drawable.move_22, 0.40f, 0.24f),
    UPPER_BACK("등·가슴", "굽은 등 · 답답한 가슴", R.drawable.move_25, 0.62f, 0.22f),
}

/**
 * 빠른 운동 전에 묻는 오늘의 통증 정도.
 * 정도에 따라 같은 부위라도 동작 조합이 달라진다 (아플수록 부드럽게, 가벼울수록 깊게).
 */
enum class PainLevel(val label: String, val sub: String, val reason: String) {
    MILD("살짝 뻐근해요", "가동 범위를 넓히는 조합", "가볍게 뻐근한 날이라 가동 범위를 크게 쓰는 동작으로 깊게 풀었어요."),
    MODERATE("꽤 불편해요", "풀어 주고 바로잡는 조합", "꽤 불편한 날이라 굳은 곳을 풀고 자세를 바로잡는 기본 동작으로 구성했어요."),
    SEVERE("많이 아파요", "부드럽게 달래는 조합", "많이 아픈 날이라 깊게 늘리는 동작은 빼고, 작고 부드러운 동작 위주로 구성했어요."),
}

data class Routine(
    val part: BodyPart,
    val title: String,
    val moves: List<Move>,
    /** 빠른 운동에서 고른 통증 정도 (없으면 날짜별 조합) */
    val pain: PainLevel? = null,
) {
    val totalSeconds: Int get() = moves.sumOf { it.seconds }
}

object Routines {

    val all: List<Routine> = listOf(
        Routine(
            BodyPart.LOWER_BACK, "허리 운동",
            listOf(
                Move(
                    "힙힌지 숙이기", "허리 펴고 엉덩이로 숙이기",
                    "두 손은 무릎 위, 허리를 곧게 편 채\n엉덩이를 뒤로 빼며 숙였다가 천천히 일어나요.",
                    "엉덩이를 시트 앞쪽으로 당겨 앉고, 두 손을 무릎 위에 올려요. 허리를 곧게 편 채 엉덩이를 뒤로 빼듯이 상체를 앞으로 숙였다가, 엉덩이에 힘을 주며 천천히 일어나 주세요. 열 번 반복합니다.",
                    "천천히 10회",
                    listOf("등이 둥글게 말리지 않게", "목은 허리와 일직선"),
                    R.drawable.move_00, R.drawable.move_00_a, R.raw.v00,
                ),
                Move(
                    "좌석 비틀기", "다리 꼬고 가슴부터 돌리기",
                    "오른 다리를 꼬고 왼손으로 오른 무릎 바깥을 잡은 뒤\n오른손을 등받이에 걸고 가슴부터 돌려요.",
                    "오른쪽 다리를 왼쪽 다리 위로 꼬고, 왼손으로 오른쪽 무릎 바깥을 잡아요. 오른손은 등받이 뒤에 걸고, 가슴부터 오른쪽으로 천천히 돌려 주세요. 십오 초 버틴 뒤 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("골반은 정면에 고정", "허리가 아니라 가슴부터 회전"),
                    R.drawable.move_01, R.drawable.move_01_a, R.raw.v01, switchSides = true,
                ),
                Move(
                    "무릎 가슴으로 당기기", "한쪽 무릎을 감싸 당기기",
                    "등을 등받이에 붙이고 한쪽 무릎을 들어\n정강이를 두 손으로 감싸 가슴 쪽으로 당겨요.",
                    "등을 등받이에 붙이고, 두 손을 한쪽 무릎 위에 올려요. 그 무릎을 들어 정강이를 두 손으로 감싸고, 가슴 쪽으로 천천히 당겨 주세요. 십오 초 버틴 뒤 반대쪽 무릎입니다.",
                    "좌우 15초씩",
                    listOf("등은 등받이에서 떨어지지 않게", "반대쪽 발은 바닥에 단단히"),
                    R.drawable.move_02, R.drawable.move_02_a, R.raw.v02, switchSides = true,
                ),
                Move(
                    "옆구리 늘리기", "한 팔 위로, 옆으로 기울이기",
                    "왼손으로 시트 옆을 잡고 오른팔을 머리 위로 뻗은 뒤\n상체를 왼쪽으로 기울여 오른쪽 옆구리를 늘려요.",
                    "왼손으로 시트 방석 왼쪽 옆을 잡고, 오른팔을 머리 위로 쭉 뻗어요. 엉덩이는 시트에 붙인 채 상체를 왼쪽으로 천천히 기울여서 오른쪽 옆구리를 늘려 주세요. 십오 초 버틴 뒤 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("양쪽 엉덩이는 시트에 그대로", "앞으로 숙이지 말고 옆으로만"),
                    R.drawable.move_12, R.drawable.move_12_a, R.raw.v12, switchSides = true,
                ),
                Move(
                    "상체 숙여 허리 늘리기", "정강이 따라 손 내리며 숙이기",
                    "두 발을 넓게 벌리고 두 손을 무릎에 올린 뒤\n손을 정강이로 미끄러뜨리며 등을 둥글게 숙여요.",
                    "두 발을 어깨너비보다 조금 넓게 벌리고, 두 손을 무릎 위에 올려요. 손을 정강이 쪽으로 천천히 미끄러뜨리면서 등을 둥글게 말아 상체를 숙여 주세요. 십오 초 버티고, 손으로 무릎을 밀면서 천천히 올라옵니다.",
                    "15초 × 2회",
                    listOf("고개는 힘을 빼고 툭", "올라올 땐 손으로 무릎 밀기"),
                    R.drawable.move_13, R.drawable.move_13_a, R.raw.v13,
                ),
            ),
        ),
        Routine(
            BodyPart.NECK, "목·어깨 운동",
            listOf(
                Move(
                    "턱 당기기", "턱을 수평으로 뒤로 밀기",
                    "시선은 정면 그대로, 턱을 수평으로 뒤로 밀어\n뒤통수를 헤드레스트에 붙여요.",
                    "평소처럼 머리가 앞으로 나와 있는 자세에서, 두 손은 허벅지 위에 둡니다. 시선은 정면 그대로, 턱을 수평으로 뒤로 밀어 뒤통수를 헤드레스트에 붙여 주세요. 삼 초 버티고 풀기를 열 번 반복합니다.",
                    "3초 버티기 10회",
                    listOf("고개를 숙이지 말고 수평으로", "턱 밑이 살짝 접히면 정답"),
                    R.drawable.move_03, R.drawable.move_03_a, R.raw.v03,
                ),
                Move(
                    "목 옆 늘리기", "시트를 잡고 귀를 어깨로",
                    "오른손으로 시트 방석 옆을 잡고\n왼쪽 귀를 왼쪽 어깨 쪽으로 천천히 기울여요.",
                    "오른손으로 시트 방석 옆을 잡고, 고개를 똑바로 세워요. 왼쪽 귀를 왼쪽 어깨 쪽으로 천천히 기울여 주세요. 오른쪽 어깨는 아래로 눌러 둡니다. 십오 초 뒤 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("잡은 쪽 어깨는 아래로 누르기", "고개가 앞으로 숙여지지 않게"),
                    R.drawable.move_04, R.drawable.move_04_a, R.raw.v04, switchSides = true,
                ),
                Move(
                    "날개뼈 모으기", "팔꿈치를 뒤로 당겨 가슴 열기",
                    "팔을 앞으로 뻗었다가 팔꿈치를 90도로 굽혀\n옆구리 뒤로 당기며 날개뼈를 꽉 모아요.",
                    "두 팔을 앞으로 뻗어 핸들을 잡듯이 둡니다. 팔꿈치를 구십 도로 굽혀 옆구리 뒤로 끌어당기고, 날개뼈를 꽉 모아 가슴을 열어 주세요. 어깨는 귀에서 멀리. 삼 초 버티고 풀기를 열 번 반복합니다.",
                    "3초 버티기 10회",
                    listOf("어깨는 귀에서 멀리", "가슴을 앞으로 활짝 열기"),
                    R.drawable.move_05, R.drawable.move_05_a, R.raw.v05,
                ),
                Move(
                    "어깨 으쓱 내리기", "어깨를 귀까지 올렸다 내리기",
                    "두 팔을 옆으로 툭 내리고\n어깨를 귀까지 올렸다가 아래로 쭉 끌어내려요.",
                    "두 팔을 몸 옆으로 편하게 늘어뜨려요. 어깨를 귀 쪽으로 최대한 끌어올려 이 초 버티고, 툭 떨어뜨린 다음 어깨를 아래로 쭉 끌어내려 주세요. 고개는 움직이지 않습니다. 열 번 반복합니다.",
                    "10회",
                    listOf("고개는 움직이지 않기", "내릴 때 날개뼈를 아래로"),
                    R.drawable.move_14, R.drawable.move_14_a, R.raw.v14,
                ),
                Move(
                    "목 대각선 늘리기", "겨드랑이 보며 목 뒤 늘리기",
                    "오른손은 시트 옆을 잡고 왼손을 뒤통수에 얹은 뒤\n왼쪽 겨드랑이를 내려다보며 손 무게로 지그시 눌러요.",
                    "오른손으로 시트 오른쪽 옆을 잡고, 왼손은 뒤통수에 얹어요. 고개를 왼쪽으로 돌려 왼쪽 겨드랑이를 내려다보고, 왼손 무게로만 지그시 눌러 오른쪽 목 뒤를 늘려 주세요. 손으로 당기지는 않아요. 십오 초 뒤 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("손으로 당기지 말고 무게만", "반대쪽 어깨는 아래로"),
                    R.drawable.move_15, R.drawable.move_15_a, R.raw.v15, switchSides = true,
                ),
            ),
        ),
        Routine(
            BodyPart.HIP, "고관절 운동",
            listOf(
                Move(
                    "앉아서 4자 스트레칭", "발목을 무릎에 올리고 숙이기",
                    "오른 발목을 왼 무릎 위에 올려 4자를 만들고\n허리를 편 채 가슴을 앞으로 숙여요.",
                    "오른쪽 발목을 왼쪽 무릎 위에 올려 숫자 사 모양을 만들고, 두 손은 올린 다리 위에 둡니다. 허리를 곧게 편 채 가슴을 앞으로 천천히 숙여 주세요. 오른쪽 엉덩이 바깥이 당기면 좋습니다. 십오 초 뒤 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("등을 둥글게 말지 않기", "엉덩이 바깥이 당기는 만큼만"),
                    R.drawable.move_06, R.drawable.move_06_a, R.raw.v06, switchSides = true,
                ),
                Move(
                    "무릎 좌우로 넘기기", "붙인 무릎을 좌우로",
                    "두 무릎과 발을 붙이고 시트 양옆을 잡은 뒤\n상체는 그대로 무릎만 좌우로 넘겨요.",
                    "두 무릎과 발을 붙이고, 두 손은 시트 양옆을 잡아요. 상체는 그대로 두고, 붙인 두 무릎을 왼쪽으로 천천히 넘겼다가 가운데로, 다시 오른쪽으로 넘겨 주세요. 좌우 열 번 반복합니다.",
                    "좌우 10회",
                    listOf("두 무릎이 떨어지지 않게", "상체와 어깨는 정면 고정"),
                    R.drawable.move_07, R.drawable.move_07_a, R.raw.v07,
                ),
                Move(
                    "무릎으로 주먹 조이기", "주먹 끼우고 무릎 꽉 조이기",
                    "두 주먹을 세로로 겹쳐 무릎 사이에 끼우고\n무릎으로 꽉 조여 5초 버텨요.",
                    "두 발을 바닥에 붙이고, 두 주먹을 세로로 겹쳐 무릎 사이에 끼워요. 허리를 세우고, 무릎으로 주먹을 꽉 조여 주세요. 허벅지 안쪽과 엉덩이에 힘이 들어가면 오 초 버티고, 천천히 풀어요. 열 번 반복합니다.",
                    "5초 버티기 10회",
                    listOf("허리는 곧게 세우기", "허벅지 안쪽에 힘 꽉"),
                    R.drawable.move_08, R.drawable.move_08_a, R.raw.v08,
                ),
                Move(
                    "무릎 들어 올리기", "시트 잡고 무릎을 가슴 쪽으로",
                    "등을 기대고 두 손으로 시트 양옆을 잡은 뒤\n한쪽 무릎을 가슴 쪽으로 들어 올렸다 내려요.",
                    "등을 등받이에 기대고, 두 손으로 시트 양옆을 잡아요. 오른쪽 무릎을 가슴 쪽으로 천천히 들어 올려 이 초 버티고 살짝 내려 주세요. 그다음 왼쪽. 상체가 뒤로 젖혀지지 않게, 좌우 번갈아 열 번씩 합니다.",
                    "좌우 번갈아 10회",
                    listOf("상체가 뒤로 젖혀지지 않게", "내릴 때 발이 쿵 떨어지지 않게"),
                    R.drawable.move_16, R.drawable.move_16_a, R.raw.v16,
                ),
                Move(
                    "햄스트링 늘리기", "다리 펴고 가슴 숙이기",
                    "오른 다리를 앞으로 쭉 펴서 뒤꿈치만 바닥에 대고\n허리를 편 채 가슴을 앞으로 숙여요.",
                    "엉덩이를 시트 앞쪽으로 조금 당겨 앉아요. 오른 다리를 앞으로 쭉 펴서 뒤꿈치만 바닥에 대고, 발끝은 위로 세워요. 두 손은 왼쪽 허벅지 위에 두고, 허리를 곧게 편 채 가슴을 앞으로 숙여 주세요. 허벅지 뒤가 당기면 십오 초 버티고 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("등을 둥글게 말지 않기", "발끝은 위로 세우기"),
                    R.drawable.move_17, R.drawable.move_17_a, R.raw.v17, switchSides = true,
                ),
            ),
        ),
        Routine(
            BodyPart.SHIN_ANKLE, "정강이·발목 운동",
            listOf(
                Move(
                    "발목 원 그리기", "발을 들고 엄지로 원 그리기",
                    "등을 기대고 오른발을 살짝 든 뒤\n발목만 움직여 엄지발가락으로 크게 원을 그려요.",
                    "등을 기대고 오른발을 바닥에서 살짝 들어요. 발목만 움직여 엄지발가락으로 크게 원을 그려 주세요. 다섯 번 돌리고 반대 방향, 그다음 왼발입니다.",
                    "5바퀴 × 양방향",
                    listOf("무릎은 고정, 발목만 회전", "원을 최대한 크게"),
                    R.drawable.move_09, R.drawable.move_09_a, R.raw.v09, switchSides = true,
                ),
                Move(
                    "발끝 들기", "뒤꿈치는 바닥, 발끝만 위로",
                    "두 발을 바닥에 편하게 두고\n뒤꿈치는 붙인 채 발끝만 정강이 쪽으로 들어요.",
                    "두 발을 바닥에 편하게 내려놓아요. 뒤꿈치는 바닥에 붙인 채 발끝을 정강이 쪽으로 최대한 들어 올렸다가 내려 주세요. 열다섯 번 반복합니다.",
                    "15회",
                    listOf("뒤꿈치는 바닥에서 떼지 않기", "정강이 앞이 당기는 느낌"),
                    R.drawable.move_10, R.drawable.move_10_a, R.raw.v10,
                ),
                Move(
                    "뒤꿈치 들기", "발끝은 바닥, 뒤꿈치만 위로",
                    "두 발을 바닥에 붙이고\n발끝은 둔 채 뒤꿈치를 최대한 높이 들었다 내려요.",
                    "두 발을 바닥에 붙이고, 손은 허벅지 위에 둡니다. 발끝은 바닥에 둔 채 뒤꿈치를 최대한 높이 들었다가 천천히 내려 주세요. 열다섯 번 반복합니다.",
                    "15회",
                    listOf("내릴 때 천천히", "종아리에 힘이 들어가는 느낌"),
                    R.drawable.move_11, R.drawable.move_11_a, R.raw.v11,
                ),
                Move(
                    "발목 안팎 굴리기", "발바닥 바깥·안쪽 날로 서기",
                    "두 발을 바닥에 두고\n발바닥 바깥쪽 날, 안쪽 날로 번갈아 서요.",
                    "두 발을 골반 너비로 바닥에 두고, 손은 허벅지 위에 둡니다. 발바닥 바깥쪽 날로 천천히 섰다가, 다시 안쪽 날로 바꿔 서 주세요. 무릎은 크게 움직이지 않게, 좌우 열 번 반복합니다.",
                    "좌우 10회",
                    listOf("무릎은 고정, 발목만 움직이기", "천천히 끝까지 기울이기"),
                    R.drawable.move_18, R.drawable.move_18_a, R.raw.v18,
                ),
                Move(
                    "정강이 꾹꾹 누르기", "엄지로 정강이 근육 풀기",
                    "오른 발목을 왼 무릎에 올리고 두 엄지로\n정강이 바깥 근육을 무릎에서 발목까지 눌러 내려요.",
                    "오른쪽 발목을 왼쪽 무릎 위에 올려요. 두 엄지를 정강이뼈 바로 바깥 근육에 대고, 무릎 아래에서 발목까지 꾹꾹 누르며 천천히 내려가 주세요. 아픈 곳은 오 초 머물러요. 삼십 초 뒤 반대쪽입니다.",
                    "좌우 30초씩",
                    listOf("뼈가 아니라 바로 옆 근육", "아픈 곳은 5초 머물기"),
                    R.drawable.move_19, R.drawable.move_19_a, R.raw.v19, switchSides = true,
                ),
            ),
        ),
        Routine(
            BodyPart.WRIST, "손목·전완 운동",
            listOf(
                Move(
                    "손목 앞쪽 늘리기", "손가락을 몸 쪽으로 당기기",
                    "오른팔을 앞으로 쭉 펴서 손바닥을 앞으로 세우고\n왼손으로 손가락 위쪽을 몸 쪽으로 눌러 당겨요.",
                    "오른팔을 앞으로 쭉 펴고, 손바닥이 앞을 보게 손을 위로 세워요. 왼손으로 오른손 손가락 위쪽을 잡아, 몸 쪽으로 지그시 눌러 당겨 주세요. 손바닥이 아니라 손가락을 당기는 거예요. 팔 안쪽이 당기면 십오 초 버티고 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("손바닥 말고 손가락을 당기기", "팔꿈치는 끝까지 펴기"),
                    R.drawable.move_20, R.drawable.move_20_a, R.raw.v20, switchSides = true,
                ),
                Move(
                    "손목 뒤쪽 늘리기", "손등을 몸 쪽으로 누르기",
                    "오른팔을 앞으로 펴고 손목을 아래로 꺾은 뒤\n왼손으로 손등을 몸 쪽으로 눌러요.",
                    "오른팔을 앞으로 쭉 펴고, 손목을 아래로 꺾어 손등이 앞을 보게 해요. 왼손으로 오른손 손등을 감싸 몸 쪽으로 지그시 눌러 주세요. 팔 바깥쪽이 당기면 십오 초 버티고 반대쪽입니다.",
                    "좌우 15초씩",
                    listOf("팔꿈치는 끝까지 펴기", "주먹을 살짝 쥐면 더 깊게"),
                    R.drawable.move_21, R.drawable.move_21_a, R.raw.v21, switchSides = true,
                ),
                Move(
                    "주먹 쥐었다 쫙 펴기", "핸들 잡던 손 풀기",
                    "두 팔을 앞으로 뻗고\n주먹을 꽉 쥐었다가 손가락을 최대한 쫙 벌려요.",
                    "두 팔을 가슴 높이로 앞으로 뻗어요. 주먹을 꽉 쥐고 이 초, 손가락을 부채처럼 최대한 쫙 벌려 이 초. 이렇게 열다섯 번 반복합니다.",
                    "15회",
                    listOf("쥘 때는 엄지로 감싸 꽉", "펼 때는 손가락 사이 최대한"),
                    R.drawable.move_22, R.drawable.move_22_a, R.raw.v22,
                ),
                Move(
                    "손목 돌리기", "팔꿈치 고정하고 크게 원",
                    "팔꿈치를 옆구리에 붙이고 가볍게 주먹 쥔 뒤\n손목만 크게 원을 그리며 돌려요.",
                    "팔꿈치를 옆구리에 붙이고 팔을 앞으로 구십 도 굽혀요. 가볍게 주먹을 쥐고, 손목만 크게 원을 그리며 열 번 돌려 주세요. 그다음 반대 방향으로 열 번입니다.",
                    "10바퀴 × 양방향",
                    listOf("팔꿈치는 옆구리에 고정", "원을 최대한 크게"),
                    R.drawable.move_23, R.drawable.move_23_a, R.raw.v23,
                ),
                Move(
                    "합장 손목 늘리기", "손바닥 붙이고 아래로",
                    "가슴 앞에서 두 손바닥을 붙이고\n붙인 채로 명치 쪽까지 천천히 내려요.",
                    "가슴 앞에서 두 손바닥을 마주 붙여 합장해요. 손바닥이 떨어지지 않게 붙인 채, 손을 명치 쪽으로 천천히 내리면서 팔꿈치를 옆으로 벌려 주세요. 손목 안쪽이 당기면 십오 초 버팁니다. 두 번 반복합니다.",
                    "15초 × 2회",
                    listOf("손바닥 아래쪽까지 꼭 붙이기", "팔꿈치는 수평으로 벌리기"),
                    R.drawable.move_24, R.drawable.move_24_a, R.raw.v24,
                ),
            ),
        ),
        Routine(
            BodyPart.UPPER_BACK, "등·가슴 운동",
            listOf(
                Move(
                    "가슴 열기", "팔꿈치 벌리고 가슴 들기",
                    "두 손을 깍지 껴 뒤통수에 대고\n팔꿈치를 활짝 벌리며 가슴을 위로 들어요.",
                    "두 손을 깍지 껴 뒤통수에 대요. 팔꿈치를 옆으로 활짝 벌리고, 등 윗부분을 등받이에 걸치듯 가슴을 천장 쪽으로 들어 주세요. 오 초 버티고 풀기를 열 번 반복합니다.",
                    "5초 버티기 10회",
                    listOf("허리가 아니라 가슴을 들기", "턱은 들지 말고 정면"),
                    R.drawable.move_25, R.drawable.move_25_a, R.raw.v25,
                ),
                Move(
                    "등 둥글게 밀기", "깍지 끼고 앞으로 쭉",
                    "두 손을 깍지 껴 앞으로 쭉 밀고\n등을 둥글게 말아 날개뼈 사이를 벌려요.",
                    "두 손을 깍지 끼고 손바닥이 앞을 보게 뒤집어요. 팔을 앞으로 쭉 밀면서 등을 둥글게 말고, 고개는 팔 사이로 살짝 숙여 날개뼈 사이를 벌려 주세요. 십오 초 버티고 두 번 반복합니다.",
                    "15초 × 2회",
                    listOf("팔은 멀리, 등은 뒤로", "엉덩이는 시트에 고정"),
                    R.drawable.move_26, R.drawable.move_26_a, R.raw.v26,
                ),
                Move(
                    "팔 교차 상체 회전", "가슴만 좌우로 돌리기",
                    "두 팔을 가슴 앞에 X자로 교차해 어깨를 잡고\n골반은 정면, 가슴만 좌우로 돌려요.",
                    "두 팔을 가슴 앞에서 엑스 자로 교차해, 양손으로 반대쪽 어깨를 잡아요. 골반과 무릎은 정면에 둔 채, 가슴만 오른쪽으로 천천히 돌렸다가 가운데, 다시 왼쪽으로 돌려 주세요. 좌우 열 번 반복합니다.",
                    "좌우 10회",
                    listOf("무릎이 따라 돌지 않게", "고개는 가슴과 함께"),
                    R.drawable.move_27, R.drawable.move_27_a, R.raw.v27,
                ),
                Move(
                    "W 자세 날개뼈 내리기", "팔꿈치를 옆구리로 끌어내리기",
                    "두 팔을 머리 위로 뻗었다가\n팔꿈치를 옆구리로 끌어내려 W자를 만들어요.",
                    "두 팔을 머리 위로 쭉 뻗어요. 팔꿈치를 굽혀 옆구리 쪽으로 끌어내리면서, 날개뼈를 아래로 꽉 모아 더블유 자를 만들어 주세요. 이 초 버티고 다시 올리기를 열 번 반복합니다.",
                    "10회",
                    listOf("어깨는 귀에서 멀리", "가슴을 앞으로 내밀기"),
                    R.drawable.move_28, R.drawable.move_28_a, R.raw.v28,
                ),
                Move(
                    "팔 위로 쭉 기지개", "깍지 끼고 머리 위로",
                    "깍지 낀 손바닥을 위로 뒤집어\n머리 위로 쭉 밀어 올리며 몸을 길게 늘여요.",
                    "두 손을 깍지 끼고 손바닥이 위를 보게 뒤집어요. 머리 위로 쭉 밀어 올리면서, 정수리는 위로, 엉덩이는 시트에 눌러 몸을 길게 늘여 주세요. 십 초 버티고 세 번 반복합니다.",
                    "10초 × 3회",
                    listOf("팔꿈치를 끝까지 펴기", "갈비뼈가 앞으로 튀어나오지 않게"),
                    R.drawable.move_29, R.drawable.move_29_a, R.raw.v29,
                ),
            ),
        ),
    )

    fun of(part: BodyPart): Routine = all.first { it.part == part }

    /** 빠른 운동에 들어가는 동작 수 */
    const val QUICK_MOVES = 3

    /** 동작 설명 음성 + 준비 구령에 드는 평균 시간(초) */
    const val GUIDE_SECONDS = 22

    /**
     * 실제로 진행할 세션.
     * - 빠른 운동: 5동작 중 3동작을 날짜에 따라 돌려 가며 고른다 (매일 조금씩 다른 조합, 5일이면 모든 동작을 고르게).
     * - 전체 운동: 5동작 전부.
     * 순서는 원래 루틴 순서(가벼운 동작 → 깊은 동작)를 지킨다.
     */
    fun session(part: BodyPart, full: Boolean, dayOfYear: Int): Routine {
        val r = of(part)
        if (full || r.moves.size <= QUICK_MOVES) return r
        val n = r.moves.size
        val pick = (0 until QUICK_MOVES).map { (dayOfYear + it) % n }.sorted()
        return r.copy(moves = pick.map { r.moves[it] })
    }

    /**
     * 통증 정도별 3동작 조합 (각 부위 5동작의 인덱스).
     * 재활 원칙: 통증이 클수록 등척성·작은 가동 범위 → 통증이 작을수록 큰 가동 범위·깊은 스트레칭.
     */
    private val painCombos: Map<BodyPart, Map<PainLevel, List<Int>>> = mapOf(
        // 0 힙힌지 1 좌석 비틀기 2 무릎 당기기 3 옆구리 4 상체 숙여 늘리기
        BodyPart.LOWER_BACK to mapOf(
            PainLevel.SEVERE to listOf(2, 3, 0),
            PainLevel.MODERATE to listOf(0, 2, 1),
            PainLevel.MILD to listOf(1, 3, 4),
        ),
        // 0 턱 당기기 1 목 옆 2 날개뼈 모으기 3 어깨 으쓱 4 목 대각선
        BodyPart.NECK to mapOf(
            PainLevel.SEVERE to listOf(0, 3, 2),
            PainLevel.MODERATE to listOf(0, 1, 2),
            PainLevel.MILD to listOf(1, 4, 2),
        ),
        // 0 4자 1 무릎 좌우 2 무릎 주먹 조이기 3 무릎 들기 4 햄스트링
        BodyPart.HIP to mapOf(
            PainLevel.SEVERE to listOf(1, 2, 3),
            PainLevel.MODERATE to listOf(1, 0, 3),
            PainLevel.MILD to listOf(0, 4, 2),
        ),
        // 0 발목 원 1 발끝 들기 2 뒤꿈치 들기 3 안팎 굴리기 4 정강이 누르기
        BodyPart.SHIN_ANKLE to mapOf(
            PainLevel.SEVERE to listOf(4, 0, 1),
            PainLevel.MODERATE to listOf(0, 1, 2),
            PainLevel.MILD to listOf(2, 3, 1),
        ),
        // 0 손목 앞쪽 1 손목 뒤쪽 2 주먹 쥐었다 펴기 3 손목 돌리기 4 합장
        BodyPart.WRIST to mapOf(
            PainLevel.SEVERE to listOf(2, 3, 0),
            PainLevel.MODERATE to listOf(0, 1, 2),
            PainLevel.MILD to listOf(0, 1, 4),
        ),
        // 0 가슴 열기 1 등 둥글게 2 팔 교차 회전 3 W 4 기지개
        BodyPart.UPPER_BACK to mapOf(
            PainLevel.SEVERE to listOf(1, 3, 4),
            PainLevel.MODERATE to listOf(0, 3, 1),
            PainLevel.MILD to listOf(0, 2, 4),
        ),
    )

    /** 통증 정도에 맞춘 빠른 운동 3동작 */
    fun painSession(part: BodyPart, level: PainLevel): Routine {
        val r = of(part)
        val idx = painCombos[part]?.get(level) ?: return session(part, false, 0).copy(pain = level)
        return r.copy(moves = idx.map { r.moves[it] }, pain = level)
    }

    /** 미리보기용: 통증 정도별 동작 이름 */
    fun painPreview(part: BodyPart, level: PainLevel): List<String> = painSession(part, level).moves.map { it.name }

    /** 안내 음성까지 포함한 대략적인 소요 시간(분) */
    fun approxMinutes(moves: Int, perMoveSeconds: Int): Int =
        ((moves * (perMoveSeconds + GUIDE_SECONDS) + 10 + 59) / 60)
}
