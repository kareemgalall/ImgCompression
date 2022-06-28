import java.util.ArrayList;
import java.util.Collections;

public class vector_quantization {
    ArrayList<float[][]> vectors;
    int vectorSize;
    int bookSize;
    image img;
    ArrayList<float[][]> codex;
    ArrayList[] IDs;
    int codexsize;
    ArrayList<Integer> parent;
    static int i;

    public vector_quantization(){}

    public vector_quantization(int vectorSize, int bookSize) {
        this.vectorSize = vectorSize;
        this.bookSize = bookSize;
    }

    public void compress(String source, String dest) {
        System.out.println("Compressing");
        this.img = new image(source);
        vectors = new ArrayList<>();
        //split image to vectors
        for (int i = 0; i < img.w; i += vectorSize) {
            for (int j = 0; j < img.h; j += vectorSize) {
                float[][] tmp = new float[vectorSize][vectorSize];
                int Row = 0;
                for (int k = i; k < i + vectorSize; k++) {
                    int col = 0;
                    for (int l = j; l < j + vectorSize; l++) {
                        try {
                            tmp[Row][col++] = img.pixels[k][l];
                        } catch (Exception ignored) {
                        }
                    }
                    Row++;
                }
                vectors.add(tmp);
            }
        }
        System.out.println("Splitting");
        splitting();
        System.out.println("Encoding");
        encode(dest);
    }

    public void vectors_to_images(ArrayList<float[][]> vector, String str) {
        image tmp = new image();
        i = 0;
        for (float[][] vec : vector) {
            tmp.pixels = vec;
            tmp.toImage(".\\test\\" + str + "_%s.png".formatted(i++));
        }
    }

    public void vector_replace(float[][] vec, int i) {
        vectors.set(i, vec);
    }

    public void encode(String dest) {
        StringBuilder data = new StringBuilder();
        String[] lables = new String[codex.size()];
        for (int i = 0; i < codex.size(); i++) {
            lables[i] = fileProcessor.toBin(i, fileProcessor.log2(codex.size()));
        }

        for (int i = 0; i < vectors.size(); i++) {
            data.append(lables[findcoodbookofID(i)]);
        }

        String header = "";
        header += fileProcessor.toBin(img.w, 16);
        header += fileProcessor.toBin(img.h, 16);
        header += fileProcessor.toBin(codex.size(), 16);
        header += fileProcessor.toBin(vectorSize, 16);
        StringBuilder coodbook = new StringBuilder();
        for (float[][] code : codex) {
            for (int i = 0; i < vectorSize; i++) {
                for (int j = 0; j < vectorSize; j++) {
                    coodbook.append(fileProcessor.toBin((int) code[i][j], 8));
                }
            }
        }

        data.insert(0, header + coodbook);
        int extraLen = 8 - (data.length() % 8);
        if (extraLen == 8)
            extraLen = 0;

        data = new StringBuilder(fileProcessor.binaryStringToBits(data.toString(), extraLen));
        data.insert(0, String.valueOf((byte) extraLen));
        fileProcessor.writeToFile(data.toString(), dest);
    }

    private void decode(String source) {

        img = new image();
        String fileData = fileProcessor.fileToString(source);
        int extralen;
        System.out.println(extralen = Integer.parseInt(fileData.substring(0, 1)));
        fileData = fileData.substring(1);

        System.out.print((img.w = Integer.parseInt(fileProcessor.bitsToBinaryString(fileData.substring(0, 2), 0), 2)) + " x ");
        fileData = fileData.substring(2);

        System.out.println(img.h = Integer.parseInt(fileProcessor.bitsToBinaryString(fileData.substring(0, 2), 0), 2));
        fileData = fileData.substring(2);

        System.out.println("codebook Size =" + (codexsize = Integer.parseInt(fileProcessor.bitsToBinaryString(fileData.substring(0, 2), 0), 2)));
        fileData = fileData.substring(2);

        System.out.println("vector Size =" + (vectorSize = Integer.parseInt(fileProcessor.bitsToBinaryString(fileData.substring(0, 2), 0), 2)));
        fileData = fileData.substring(2);

        codex = new ArrayList<>();
        for (int codes = 0; codes < codexsize; codes++) {
            float[][] tmp = new float[vectorSize][vectorSize];
            for (int i = 0; i < vectorSize; i++) {
                for (int j = 0; j < vectorSize; j++) {
                    tmp[i][j] = Integer.parseInt(fileProcessor.bitsToBinaryString(fileData.substring(0, 1), 0), 2);
                    fileData = fileData.substring(1);
                }
            }
            codex.add(tmp);
        }
        parent = new ArrayList<>();
        int size = fileProcessor.log2(codexsize);
        fileData = fileProcessor.bitsToBinaryString(fileData, extralen);
        while (fileData.length() >= size) {
            parent.add(Integer.parseInt(fileData.substring(0, size), 2));
            fileData = fileData.substring(size);
        }
        System.out.println("number of a labels =" + parent.size());
    }

    public void decompress(String source, String dest) {
        System.out.println("Decompressing");
        System.out.println("Decoding");
        decode(source);
        int w = 0;
        System.out.println("Building Image");
        img.pixels = new float[img.w][img.h];
        for (int i = 0; i < img.w; i += vectorSize) {
            for (int j = 0; j < img.h; j += vectorSize) {
                float[][] tmp;
                try {
                    tmp = codex.get(parent.get(w));
                } catch (Exception e) {
                    System.out.println("not found " + e.getMessage());
                    w++;
                    continue;
                }
                int Row = 0;
                for (int k = i; k < i + vectorSize; k++) {
                    int col = 0;
                    for (int l = j; l < j + vectorSize; l++) {
                        try {
                            img.pixels[k][l] = tmp[Row][col++];
                        } catch (Exception ignored) {
                        }
                    }
                    Row++;
                }
                w++;
            }
        }
        img.toImage(dest);
        System.out.println("Complete");
    }

    public int findcoodbookofID(int ID) {
        for (int node = 0; node < codex.size(); node++) {
            if (IDs[node] != null && IDs[node].contains(ID)) {
                return node;
            }
        }
        return -1;
    }

    public void splitting() {
        //Get average vector
        float[][] averageVec;           //Average
        averageVec = getAverageOfVectors(vectors);

        codex = new ArrayList<>();
        IDs = new ArrayList[bookSize];
        codex.add(averageVec);
        int limit = 0;
        while (limit++ < vectors.size() && codex.size() < vectors.size() && codex.size() < bookSize) {
            int size = codex.size();
            for (int i = 0; i < size; i++) {
                float[][] tmp = addToVector(codex.remove(0), 0);
                codex.add(tmp);
                codex.add(addToVector(tmp, 1));
            }
            size = vectors.size();
            ArrayList[] tmpIDs = new ArrayList[codex.size()];
            for (int i = 0; i < size; i++) {
                ArrayList<Integer> MESs = new ArrayList<>();
                for (float[][] code : codex) {
                    MESs.add(MSE(code, vectors.get(i)));
                }
                int possion = getindexofmin(MESs);
                if (tmpIDs[possion] == null)
                    tmpIDs[possion] = new ArrayList<>();
                tmpIDs[possion].add(i);
            }
            for (int i = 0; i < codex.size(); i++) {
                if (tmpIDs[i] == null) {
                    codex.remove(i);
                    for (int j = i; i < codexsize; i++) {
                        tmpIDs[j] = tmpIDs[j + 1];
                    }
                    i--;
                } else
                    codex.set(i, AvgbyID(tmpIDs[i]));
            }
            IDs = tmpIDs;
        }
        int size = vectors.size();
        ArrayList[] tmpIDs = new ArrayList[codex.size()];
        for (int i = 0; i < size; i++) {
            ArrayList<Integer> MESs = new ArrayList<>();
            for (float[][] code : codex) {
                MESs.add(MSE(code, vectors.get(i)));
            }
            int possion = getindexofmin(MESs);
            if (tmpIDs[possion] == null)
                tmpIDs[possion] = new ArrayList<>();
            tmpIDs[possion].add(i);
        }
        IDs = tmpIDs;
    }

    public int getindexofmin(ArrayList<Integer> a) {
        return a.indexOf(Collections.min(a));
    }

    public float[][] AvgbyID(ArrayList<Integer> a) {
        ArrayList<float[][]> vector = new ArrayList<>();
        for (Integer ID : a) {
            vector.add(vectors.get(ID));
        }
//        vectors_to_images(vector, str);
        return getAverageOfVectors(vector);
    }

    boolean vectorcomp(float[][] vec1, float[][] vec2) {
        boolean f = false;
        for (int i = 0; i < vectorSize; i++) {
            for (int j = 0; j < vectorSize; j++) {
                if (vec1[i][j] == vec2[i][j]) {
                    f = true;
                    break;
                }
            }
        }
        return f;
    }

    public float[][] getAverageOfVectors(ArrayList<float[][]> vecto) {
        float[][] averageVec = new float[vectorSize][vectorSize];
        for (float[][] vec : vecto) {
            for (int i = 0; i < vectorSize; i++) {
                for (int j = 0; j < vectorSize; j++) {
                    averageVec[i][j] += vec[i][j];
                }
            }
        }
        for (int i = 0; i < vectorSize; i++) {
            for (int j = 0; j < vectorSize; j++) {
                averageVec[i][j] /= vecto.size();
            }
        }
        return averageVec;
    }

    public void printAVector(float[][] vec) {
        for (int i = 0; i < vectorSize; i++) {
            for (int j = 0; j < vectorSize; j++) {
                System.out.print(vec[i][j] + "   ");
            }
            System.out.print("\n");
        }
    }


    public float[][] addToVector(float[][] vec, int val) {
        float[][] temp = new float[vectorSize][vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            for (int j = 0; j < vectorSize; j++) {
                int tmp = (int) Math.floor(vec[i][j] + val);
                if (tmp < 0)
                    temp[i][j] = 0;
                else temp[i][j] = Math.min(tmp, 255);
            }
        }
        return temp;
    }

    public int MSE(float[][] vec1, float[][] vec2) {
        int result = 0;

        for (int i = 0; i < vectorSize; i++) {
            for (int j = 0; j < vectorSize; j++) {
                result += Math.abs(vec1[i][j] - vec2[i][j]);
            }
        }
        return result;
    }
}
