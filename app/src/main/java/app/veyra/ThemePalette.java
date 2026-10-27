package app.veyra;
final class ThemePalette {
 static final String[] NAMES={"Cinema","Pacific","Golden hour","OLED black","Amethyst","Forest","Rosewood","Glacier"};
 static final String[] DESCRIPTIONS={"Midnight and coral","Ocean blue and sea glass","Espresso and warm amber","True black and clean white","Deep plum and lavender","Pine green and soft mint","Dark cherry and rose","Slate blue and ice"};
 static final int[][] COLORS={{0xff090d16,0xffff795f,0xff171e2b},{0xff081419,0xff70ded1,0xff142a30},{0xff16100e,0xfff2bb83,0xff2b211d},{0xff000000,0xffeeeeee,0xff1b1b1b},{0xff100c1c,0xffc5a7ff,0xff231b35},{0xff091510,0xffa5d8a2,0xff192b20},{0xff180c12,0xffffaac6,0xff301c26},{0xff0b1421,0xffa4d9ff,0xff1b2a3b}};
 static int index(int value){return value>=0&&value<NAMES.length?value:0;}
}
