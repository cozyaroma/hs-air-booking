package airline.ui;

/**
 * 페이지 담당자. 오류 메시지와 개발 도구에서 "누구에게 물어봐야 하는지" 보여줄 때 사용한다.
 * 팀원 이름이 정해지면 memberName만 바꾸면 된다.
 */
public enum PageOwner {
    CORE("공용", "통합 담당"),
    A("A", "A 담당"),
    B("B", "B 담당"),
    C("C", "C 담당");

    private final String code;
    private final String memberName;

    PageOwner(String code, String memberName) {
        this.code = code;
        this.memberName = memberName;
    }

    public String getCode() {
        return code;
    }

    public String getMemberName() {
        return memberName;
    }
}
