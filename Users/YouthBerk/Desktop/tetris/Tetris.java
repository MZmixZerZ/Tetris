import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class Tetris extends JPanel implements ActionListener {

    // ขนาดตาราง: 10 คอลัมน์, 20 แถว, แต่ละช่องกว้าง 30px
    static final int COLS = 10, ROWS = 20, SIZE = 30;

    // รูปร่างชิ้นส่วนทั้ง 7 แบบ จัดเก็บเป็น [ชิ้น][การหมุน][แถว][คอลัมน์]
    // 1 = มีบล็อก, 0 = ว่าง
    // ponytail: pieces as int[][][][] [piece][rotation][row][col], no class hierarchy needed
    static final int[][][][] PIECES = {
        {{{1,1,1,1}}, {{1},{1},{1},{1}}},                                                                  // I
        {{{1,1},{1,1}}},                                                                                    // O
        {{{0,1,0},{1,1,1}}, {{1,0},{1,1},{1,0}}, {{1,1,1},{0,1,0}}, {{0,1},{1,1},{0,1}}},                 // T
        {{{0,1,1},{1,1,0}}, {{1,0},{1,1},{0,1}}},                                                          // S
        {{{1,1,0},{0,1,1}}, {{0,1},{1,1},{1,0}}},                                                          // Z
        {{{1,0,0},{1,1,1}}, {{1,1},{1,0},{1,0}}, {{1,1,1},{0,0,1}}, {{0,1},{0,1},{1,1}}},                 // J
        {{{0,0,1},{1,1,1}}, {{1,0},{1,0},{1,1}}, {{1,1,1},{1,0,0}}, {{1,1},{0,1},{0,1}}}                  // L
    };

    // สีของแต่ละชิ้นส่วน ลำดับตรงกับ PIECES
    static final Color[] COLORS = {
        Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.GREEN, Color.RED, Color.BLUE, Color.ORANGE
    };

    int[][] board = new int[ROWS][COLS]; // ตารางหลัก: 0 = ว่าง, 1-7 = สีของชิ้นที่ล็อกแล้ว
    int curPiece, curRot, curX, curY;   // ชิ้นปัจจุบัน: ชนิด, การหมุน, ตำแหน่ง x/y
    int score;
    boolean gameOver;
    Timer timer;                         // timer ขับเคลื่อนให้ชิ้นตกลงทุก 500ms
    Random rand = new Random();

    public Tetris() {
        setPreferredSize(new Dimension(COLS * SIZE, ROWS * SIZE));
        setBackground(Color.BLACK);
        setFocusable(true); // ต้องมีเพื่อรับ keyboard event

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                // ถ้า game over รับแค่ปุ่ม R สำหรับ restart
                if (gameOver) { if (e.getKeyCode() == KeyEvent.VK_R) restart(); return; }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT  -> shift(-1, 0); // เลื่อนซ้าย
                    case KeyEvent.VK_RIGHT -> shift(1, 0);  // เลื่อนขวา
                    case KeyEvent.VK_DOWN  -> shift(0, 1);  // เลื่อนลงเร็ว
                    case KeyEvent.VK_UP    -> rotate();      // หมุน
                    case KeyEvent.VK_SPACE -> hardDrop();    // ดรอปลงทันที
                }
                repaint();
            }
        });

        restart();
    }

    // รีเซ็ตทุกอย่างแล้วเริ่มเกมใหม่
    void restart() {
        board = new int[ROWS][COLS];
        score = 0;
        gameOver = false;
        if (timer != null) timer.stop();
        timer = new Timer(500, this); // ตกทุก 500ms
        timer.start();
        spawn();
    }

    // สุ่มชิ้นใหม่แล้ววางที่ตรงกลางด้านบน
    void spawn() {
        curPiece = rand.nextInt(PIECES.length);
        curRot = 0;
        curX = COLS / 2 - PIECES[curPiece][0][0].length / 2;
        curY = 0;
        // ถ้าชิ้นใหม่วางไม่ได้ = เกมจบ
        if (!fits(curPiece, curRot, curX, curY)) { gameOver = true; timer.stop(); }
    }

    // ตรวจสอบว่าชิ้นส่วนวางได้โดยไม่ชนขอบหรือบล็อกอื่น
    boolean fits(int piece, int rot, int x, int y) {
        int[][] s = PIECES[piece][rot];
        for (int r = 0; r < s.length; r++)
            for (int c = 0; c < s[r].length; c++)
                if (s[r][c] != 0) {
                    int nx = x + c, ny = y + r;
                    if (nx < 0 || nx >= COLS || ny >= ROWS || (ny >= 0 && board[ny][nx] != 0)) return false;
                }
        return true;
    }

    // เลื่อนชิ้นตามทิศทาง ถ้าเลื่อนลงไม่ได้ให้ล็อก
    void shift(int dx, int dy) {
        if (fits(curPiece, curRot, curX + dx, curY + dy)) { curX += dx; curY += dy; }
        else if (dy > 0) lock();
    }

    // หมุนชิ้น 90 องศา ถ้าหมุนแล้วชนก็ไม่ทำอะไร
    void rotate() {
        int nr = (curRot + 1) % PIECES[curPiece].length;
        if (fits(curPiece, nr, curX, curY)) curRot = nr;
    }

    // ดรอปชิ้นลงสุดทันที
    void hardDrop() {
        while (fits(curPiece, curRot, curX, curY + 1)) curY++;
        lock();
    }

    // ล็อกชิ้นลงบอร์ด แล้ว spawn ชิ้นถัดไป
    void lock() {
        int[][] s = PIECES[curPiece][curRot];
        for (int r = 0; r < s.length; r++)
            for (int c = 0; c < s[r].length; c++)
                if (s[r][c] != 0 && curY + r >= 0)
                    board[curY + r][curX + c] = curPiece + 1; // เก็บ index+1 เพื่อให้ 0 = ว่าง
        clearLines();
        spawn();
        repaint();
    }

    // ลบแถวที่เต็มจากล่างขึ้นบน แล้วคิดคะแนน
    void clearLines() {
        int cleared = 0;
        for (int r = ROWS - 1; r >= 0; ) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) if (board[r][c] == 0) { full = false; break; }
            if (full) {
                // เลื่อนทุกแถวด้านบนลงมาหนึ่งแถว
                for (int rr = r; rr > 0; rr--) board[rr] = board[rr - 1].clone();
                board[0] = new int[COLS]; // แถวบนสุดว่างเปล่า
                cleared++;
            } else r--;
        }
        // คะแนน: 1แถว=100, 2=300, 3=500, 4=800
        score += new int[]{0, 100, 300, 500, 800}[cleared];
    }

    // timer เรียกทุก 500ms ให้ชิ้นตกลงหนึ่งช่อง
    public void actionPerformed(ActionEvent e) { shift(0, 1); repaint(); }

    // วาดทุกอย่างบนหน้าจอ
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // วาดบล็อกที่ล็อกอยู่บนบอร์ดแล้ว
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (board[r][c] != 0) drawBlock(g, c, r, COLORS[board[r][c] - 1]);

        // วาดชิ้นที่กำลังตกอยู่
        if (!gameOver) {
            int[][] s = PIECES[curPiece][curRot];
            for (int r = 0; r < s.length; r++)
                for (int c = 0; c < s[r].length; c++)
                    if (s[r][c] != 0) drawBlock(g, curX + c, curY + r, COLORS[curPiece]);
        }

        // วาดเส้นกริด
        g.setColor(new Color(40, 40, 40));
        for (int r = 0; r <= ROWS; r++) g.drawLine(0, r * SIZE, COLS * SIZE, r * SIZE);
        for (int c = 0; c <= COLS; c++) g.drawLine(c * SIZE, 0, c * SIZE, ROWS * SIZE);

        // แสดงคะแนน
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 5, 18);

        // หน้าจอ game over
        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 160)); // overlay ดำโปร่งแสง
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            FontMetrics fm = g.getFontMetrics();
            String msg = "GAME OVER";
            g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2 - 20);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            fm = g.getFontMetrics();
            String sub = "Press R to restart";
            g.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, getHeight() / 2 + 15);
        }
    }

    // วาดบล็อกเดี่ยว พร้อม highlight ขอบให้ดูเป็น 3D นิดหน่อย
    void drawBlock(Graphics g, int x, int y, Color c) {
        g.setColor(c);
        g.fillRect(x * SIZE + 1, y * SIZE + 1, SIZE - 2, SIZE - 2);
        g.setColor(c.brighter());
        g.drawRect(x * SIZE + 1, y * SIZE + 1, SIZE - 3, SIZE - 3);
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Tetris");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new Tetris());
        f.pack();
        f.setResizable(false);
        f.setLocationRelativeTo(null); // วางหน้าต่างกลางจอ
        f.setVisible(true);
    }
}
