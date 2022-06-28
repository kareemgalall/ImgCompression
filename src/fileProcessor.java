import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class fileProcessor {
    public static void writeToFile(String data, String dir) {
        try {
            File f = new File(dir);
            f.getParentFile().mkdirs();
            f.createNewFile();
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write(data);
            w.close();
        } catch (IOException e) {
            System.out.println("Writing failed");
        }
    }

    protected static String binaryStringToBits(String s, int extraLen) {
        //complete the string to multiple of 8
        s = s + "0".repeat(Math.max(0, extraLen));
        //Pack into bytes
        byte[] arr = new byte[s.length() / 8];

        for (int i = 0; i <= s.length() - 8; i += 8) {
            arr[i / 8] = (byte) Integer.parseInt(s.substring(i, i + 8), 2);
        }
        StringBuilder res = new StringBuilder();
        for (int b : arr) {
            int c = b;
            if (c < 0) c = c & 0xFF;
            res.append((char) c);
        }
        return res.toString();
    }

    protected static String bitsToBinaryString(String s, int extraLen) {

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            result.append(toBin(s.charAt(i), 8));
        }
        result = new StringBuilder(result.substring(0, result.length() - extraLen));
        return result.toString();

    }

    protected static String toBin(int x, int numOfBits) {
        if (x < 0) x *= -1;
        StringBuilder res = new StringBuilder(Integer.toBinaryString(x));
        while (res.length() < numOfBits)
            res.insert(0, "0");
        return res.toString();
    }

    protected static int log2(int x) {
        return (int) Math.ceil(Math.log(x) / Math.log(2));
    }

    public static String fileToString(String dir) {
        StringBuilder res = new StringBuilder();
        try {
            File ogFile = new File(dir);
            Scanner reader = new Scanner(ogFile);
            String tmp;
            while (reader.hasNext()) {
                tmp = reader.nextLine();
                res.append('\n');
                res.append(tmp);
            }
            res = new StringBuilder(res.substring(1)); //remove the first \n
        } catch (IOException e) {
            System.out.println("File Doesn't exist !");
            System.exit(1);
        }
        return res.toString();
    }
}
