import com.hamoid.VideoExport;
import processing.core.*; // import processing core
import processing.sound.*;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends PApplet {

    static int animationFrames = 0;

    static int FRAMES_PER_DAY = 5;

    static int TICKMARKS = 2;
    static int TICKMARK_INTERVAL = 7;
    static int DAYS_SHOWN = TICKMARK_INTERVAL * TICKMARKS;
    static float TICKMARK_THICKNESS = 4;
    static int MARGIN_WINDOW = 3;
    static int VALUE_WINDOW = 3;
    static String START_DAY;
    static String DATE_FORMAT = "yyyy-MM-dd";

    static int VERT_MARGINS = 100;
    static int HORIZ_MARGINS = 200;
    static int WINDOW_WIDTH = 1920;
    static int WINDOW_HEIGHT = 1080;

    static int WIDTH = WINDOW_WIDTH - 2 * HORIZ_MARGINS;
    static int HEIGHT = WINDOW_HEIGHT - 2 * VERT_MARGINS;

    static int TOP_VISIBLE = 10;

    static float[] maxes;
    static float[] units;
    static float[] scales;
    static Person[] people;
    static int currentDay;
    static float currentDayFloat;
    static float currentScale;

    private SoundFile sf;
    private boolean SYNC_WITH_AUDIO = true;
    private VideoExport videoExport;


    public void settings() {
        size(WINDOW_WIDTH, WINDOW_HEIGHT);
        smooth();
        fullScreen();
    }

    public void setup() {
        frameRate(60);
        PFont font = createFont("Arial", 40);
        textFont(font);

        people = DataReader.readFile("data/out.csv");
        if (TOP_VISIBLE > 0) {
            doTopVisible(people, TOP_VISIBLE);
        }
        START_DAY = people[0].startDay;
        maxes = getMaxes(people);
        scales = getScales(maxes);
        units = getUnits(maxes);
        sf = new SoundFile(this, "music/Toby Fox - Fallen Down.mp3");
        int duration = (int) sf.duration();
        sf.play();
        videoExport = new VideoExport(this, "outputtedVideo.mp4");
        if (SYNC_WITH_AUDIO) {
            frameRate(((people[0].values.length - 1) * FRAMES_PER_DAY)/ (float) duration);
            videoExport.setFrameRate(((people[0].values.length - 1) * FRAMES_PER_DAY)/ (float) duration);
        }
        videoExport.setAudioFileName("music/Toby Fox - Fallen Down.mp3");
        videoExport.startMovie();
    }

    public void draw() {
        currentDayFloat = animationFrames /(float) FRAMES_PER_DAY;
        currentDay = (int) currentDayFloat;
        float daysShown = currentDayFloat;
        currentScale = weightedAverage(scales, daysShown, MARGIN_WINDOW);
        background(0);
        drawGridLines();
        drawUnitLines();
        drawPeopleLines(currentDay, daysShown, animationFrames);
        drawAxes();
        drawLabel();
        stroke(255);
        fill(255);
        noFill();
        videoExport.saveFrame();
        if (animationFrames > (people[0].values.length - 1) * FRAMES_PER_DAY) {
            /*
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            animationFrames++;
             */
            exit();
        } else {
            Scanner sc = new Scanner(System.in);
            //String s = sc.nextLine();
            animationFrames += 1;
        }
    }

    public void drawLabel(){
        fill(255);
        noStroke();
        textAlign(LEFT);
        textSize(70);
        text("Top People Messaged on ", HORIZ_MARGINS + 60, VERT_MARGINS * 2f);
        textSize(100);
        text(frameToDay(animationFrames).format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), HORIZ_MARGINS + 60, VERT_MARGINS * 3f);
    }

    public void drawGridLines() {
        noStroke();
        fill(155);
        textAlign(CENTER);
        int X = WIDTH * (FRAMES_PER_DAY * TICKMARK_INTERVAL - animationFrames % (FRAMES_PER_DAY * TICKMARK_INTERVAL))/ (FRAMES_PER_DAY * TICKMARK_INTERVAL);
        while (X < WIDTH * TICKMARKS) {
            rect(X/ (float) TICKMARKS + HORIZ_MARGINS - TICKMARK_THICKNESS/2, VERT_MARGINS, TICKMARK_THICKNESS, HEIGHT);
            textSize(30);
            text(frameToDay(animationFrames).minusDays(((long) WIDTH * TICKMARKS - X) * TICKMARK_INTERVAL / WIDTH).format(DateTimeFormatter.ofPattern("MM/dd/YYYY")), X/TICKMARKS + HORIZ_MARGINS, VERT_MARGINS - 10);
            X += WIDTH;
        }
    }

    public void drawUnitLines() {
        float preferredUnit = Math.max(weightedAverage(units, currentDayFloat, 3), 0);
        float unitRem = (float) (preferredUnit % 1.0);
        if (unitRem < 0.001){
            unitRem = 0;
        } else if (unitRem >= 0.999){
            unitRem = 0;
            preferredUnit = PApplet.ceil(preferredUnit);
        }
        long thisUnit = unitIndexToUnit(preferredUnit);
        long nextUnit = unitIndexToUnit(preferredUnit + 1);
        drawTickMarksOfUnit(thisUnit, 255 - unitRem*255);
        if (unitRem >= 0.001){
            drawTickMarksOfUnit(nextUnit, unitRem * 255);
        }
    }

    void drawTickMarksOfUnit(float u, float alpha){
        for(float v = 0; v < currentScale; v += u){
            float y = valueToY(v, currentScale);
            fill(100, 100, 100, alpha);
            rect(HORIZ_MARGINS, y - TICKMARK_THICKNESS/2, WIDTH, TICKMARK_THICKNESS);
            textAlign(RIGHT);
            textSize(30);
            NumberFormat nf = NumberFormat.getCompactNumberInstance();
            nf.setMaximumFractionDigits(1);
            text(nf.format(v), HORIZ_MARGINS - 20, y + 13);
        }
    }

    public void drawPeopleLines(int currentDay, float daysShown, int animationFrames) {
        for (int p = 0; p < people.length; p++) {
            Person pe = people[p];
            //current lines
            //past lines
            noFill();
            beginShape();
            stroke(pe.red, pe.green, pe.blue);
            strokeWeight(4);
            boolean shape = true;
            for (int i = Math.max(0, currentDay - (int) daysShown); i <= Math.min(currentDay, pe.values.length - 1); i++) {
                if (!pe.shown[i]){
                    if (shape) {
                        shape = false;
                        float val = weightedAverage(pe.values, i, VALUE_WINDOW);
                        float X = dayToX(i, animationFrames, daysShown);
                        float Y = valueToY(val, currentScale);
                        vertex(X, Y);
                        endShape();
                    }
                    continue;
                } else if (!shape) {
                    beginShape();
                    shape=true;
                }
                float val = weightedAverage(pe.values, i, VALUE_WINDOW);
                float X = dayToX(i, animationFrames, daysShown);
                float Y = valueToY(val, currentScale);
                if (X <= HORIZ_MARGINS) {
                    X = HORIZ_MARGINS;
                    Y = PApplet.lerp(valueToY(weightedAverage(pe.values, i, VALUE_WINDOW), currentScale), valueToY(weightedAverage(pe.values, i + 1, VALUE_WINDOW), currentScale), currentDayFloat % 1);
                }
                if (val < 1 && shape) {
                    vertex(X, Y);
                    endShape();
                    shape = false;
                    continue;
                }
                vertex(X, Y);
                if (currentDay < pe.values.length && i == currentDay) {
                    if (!pe.shown[currentDay]){
                        continue;
                    }
                    X = WIDTH + HORIZ_MARGINS;
                    Y = PApplet.lerp(valueToY(weightedAverage(pe.values, currentDay, VALUE_WINDOW), currentScale), valueToY(weightedAverage(pe.values, currentDay + 1, VALUE_WINDOW), currentScale), currentDayFloat % 1);
                    vertex(X, Y);
                    fill(pe.red, pe.green, pe.green);
                    circle(X, Y, 10);
                    textAlign(LEFT);
                    textSize(15);
                    text(pe.name, X + 10, Y + 5);
                    noFill();
                    break;
                }
            }
            if (shape) {
                endShape();
            }
        }
    }

    public void drawAxes() {
        noStroke();
        fill(255);
        rect(HORIZ_MARGINS - TICKMARK_THICKNESS/2, VERT_MARGINS, TICKMARK_THICKNESS, HEIGHT);
        textSize(30);
        //text(frameToDay(animationFrames).minusDays(TICKMARKS * TICKMARK_INTERVAL).format(DateTimeFormatter.ofPattern("MM/dd")), HORIZ_MARGINS, VERT_MARGINS - 10);
        rect(HORIZ_MARGINS, HEIGHT + VERT_MARGINS - TICKMARK_THICKNESS/2, WIDTH, TICKMARK_THICKNESS);
    }

    static public void main(String[] args) {
        PApplet.main(Main.class, WIDTH + "", HEIGHT + "");
    }

    static public String frameToDayStr(int frame) {
        return frameToDay(frame).format(DateTimeFormatter.ofPattern("MM/dd"));
    }

    static public LocalDate frameToDay(int frame) {
        return LocalDate.parse(START_DAY, DateTimeFormatter.ofPattern(DATE_FORMAT)).plusDays(frame/FRAMES_PER_DAY);
    }

    float weightedAverage(float[] a, float index, float WINDOW_WIDTH){
        index = PApplet.min(a.length - 1, PApplet.max(0, index));
        int startIndex = PApplet.min(PApplet.max(0, PApplet.ceil(index-WINDOW_WIDTH)), a.length - 1);
        int endIndex = PApplet.min(PApplet.floor(index+WINDOW_WIDTH), a.length-1);
        float counter = 0;
        float summer = 0;

        for (int d = startIndex; d <= endIndex; d++){
            float val = a[d];
            float weight = (float) (0.5 + 0.5 * PApplet.cos((d-index)/WINDOW_WIDTH * PI));
            counter += weight;
            summer += val*weight;
        }
        if (summer == 0 && counter == 0) {
            throw new RuntimeException("WA returned NaN");
        }
        return summer/counter;
    }

    public float valueToY(float val, float currentScale) {
        return -val/currentScale * HEIGHT + HEIGHT + VERT_MARGINS;
    }

    public float[] getScales(float[] maxes) {
        float[] scales = new float[maxes.length];
        float[] windowedMaxes = new float[maxes.length];
        PriorityQueue<Float> maxQueue = new PriorityQueue<>(Comparator.reverseOrder());
        for (int d = 0; d < maxes.length; d++) {

            if (d > TICKMARKS * TICKMARK_INTERVAL) {
                maxQueue.remove(maxes[d - TICKMARKS * TICKMARK_INTERVAL]);
            }
            maxQueue.add(maxes[d]);
            windowedMaxes[d] = maxQueue.peek();
        }
        for (int d = 0; d < maxes.length; d++) {
            scales[d] = getScale(windowedMaxes, d);
        }
        return scales;
    }

    public float[] getUnits(float[] maxes) {
        float[] unitChoices = new float[maxes.length];
        for (int d = 0; d < maxes.length; d++){
            float scale = scales[d];
            float logThirdScale = (float) Math.log10(scale/3);
            int num = (int) logThirdScale * 3;
            if (logThirdScale % 1 > Math.log10(5)) {
                num += 2;
            } else if (logThirdScale % 1 > Math.log10(2)) {
                num += 1;
            }
            unitChoices[d] = num;
        }
        return unitChoices;
    }


    public float[] getMaxes(Person[] people) {
        float[] maxes = new float[people[0].values.length];
        for (int d = 0; d < maxes.length; d++) {
            for (Person p : people) {
                if (p.getValue(d) > maxes[d]) {
                    maxes[d] = p.getValue(d);
                }
            }
        }
        return maxes;
    }

    public float getScale(float[] maxes, float d) {
        return weightedAverage(maxes, d, MARGIN_WINDOW) * 1.2f;
    }

    public float dayToX(int day, int currentFrame, float daysShown) {
        return (float) (WIDTH + HORIZ_MARGINS - ((currentFrame % FRAMES_PER_DAY)/ (double) FRAMES_PER_DAY - (day - currentDay)) * WIDTH/(double) (daysShown));
    }

    public long unitIndexToUnit(float u) {
        long startingNum = 1;
        if (u % 3 >= 2) {
            startingNum = 5;
        } else if (u % 3 >= 1) {
            startingNum = 2;
        }
        for (int i = 0; i < (int) u/3; i++) {
            startingNum *= 10;
        }
        return startingNum;
    }

    static class Person {

        private float[] values;
        private boolean[] shown;
        private String name;

        private float red;
        private float green;
        private float blue;

        private String startDay;

        private static int totalCount = 0;
        private int count;

        Person(float[] values, String name, String startDay) {
            this.values = values;
            this.name = name;
            this.startDay = startDay;
            red = (float) (Math.random() * 155 + 100);
            green = (float) (Math.random() * 155 + 100);
            blue = (float) (Math.random() * 155 + 100);
            this.count = totalCount++;
            shown = new boolean[values.length];
            Arrays.fill(shown, true);
        }

        Person() {

        }

        public float getValue(int day) {
            return values[day];
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValues(float[] values) {
            this.values = values;
        }

    }

    public void doTopVisible(Person[] people, int topVisible) {
        for (int i = 0; i < people[0].values.length; i++) {
            int finalI = i;
            Arrays.stream(people).sorted((a, b) -> Float.compare(weightedAverage(b.values, finalI, VALUE_WINDOW), weightedAverage(a.values, finalI, VALUE_WINDOW))).skip(Math.min(topVisible, people.length)).forEach(pe -> pe.shown[finalI] = false);
        }
    }

}