import org.json.JSONObject;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

public class SecretFinder {

    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("input.json")));
        JSONObject obj = new JSONObject(content);

        int n = obj.getInt("n");
        int k = obj.getInt("k");
        JSONObject sharesJson = obj.getJSONObject("shares");

        Map<BigInteger, BigInteger> sharesMap = new HashMap<>();

        for (String key : sharesJson.keySet()) {
            BigInteger x = new BigInteger(key);
            BigInteger y = evaluate(sharesJson.getString(key));
            sharesMap.put(x, y);
        }

        List<Map.Entry<BigInteger, BigInteger>> entries = new ArrayList<>(sharesMap.entrySet());

        // Try all combinations of size k
        BigInteger secret = null;
        outer:
        for (List<Map.Entry<BigInteger, BigInteger>> combination : combinations(entries, k)) {
            List<BigInteger> xs = new ArrayList<>();
            List<BigInteger> ys = new ArrayList<>();
            for (var e : combination) {
                xs.add(e.getKey());
                ys.add(e.getValue());
            }

            try {
                BigInteger currentSecret = lagrangeInterpolation(xs, ys, BigInteger.ZERO);
                // Validate this with other combinations
                int matches = 0;
                for (List<Map.Entry<BigInteger, BigInteger>> other : combinations(entries, k)) {
                    if (other.equals(combination)) continue;
                    List<BigInteger> ox = new ArrayList<>();
                    List<BigInteger> oy = new ArrayList<>();
                    for (var e : other) {
                        ox.add(e.getKey());
                        oy.add(e.getValue());
                    }
                    try {
                        BigInteger checkSecret = lagrangeInterpolation(ox, oy, BigInteger.ZERO);
                        if (checkSecret.equals(currentSecret)) matches++;
                    } catch (Exception ignored) {}
                }

                if (matches >= 1) {  // Confirmed from at least 1 other subset
                    secret = currentSecret;
                    break outer;
                }
            } catch (Exception e) {
                // ignore bad combinations
            }
        }

        if (secret != null)
            System.out.println("Secret: " + secret);
        else
            System.out.println("No valid k-share combination found.");
    }

    static BigInteger evaluate(String expr) {
        Matcher m = Pattern.compile("(\\w+)\\((\\d+),(\\d+)\\)").matcher(expr);
        if (!m.find()) throw new RuntimeException("Invalid expression: " + expr);

        String op = m.group(1);
        BigInteger a = new BigInteger(m.group(2));
        BigInteger b = new BigInteger(m.group(3));

        return switch (op) {
            case "sum" -> a.add(b);
            case "multiply" -> a.multiply(b);
            case "hcf" -> a.gcd(b);
            case "lcm" -> a.multiply(b).divide(a.gcd(b));
            default -> throw new RuntimeException("Unsupported operation: " + op);
        };
    }

    static BigInteger lagrangeInterpolation(List<BigInteger> x, List<BigInteger> y, BigInteger at) {
        int k = x.size();
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < k; i++) {
            BigInteger xi = x.get(i), yi = y.get(i);
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = x.get(j);
                num = num.multiply(at.subtract(xj));
                den = den.multiply(xi.subtract(xj));
            }

            BigInteger term = yi.multiply(num).divide(den);
            result = result.add(term);
        }

        return result;
    }

    static <T> List<List<T>> combinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        generateCombinations(0, new ArrayList<>(), list, k, result);
        return result;
    }

    static <T> void generateCombinations(int index, List<T> current, List<T> list, int k, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = index; i <= list.size() - (k - current.size()); i++) {
            current.add(list.get(i));
            generateCombinations(i + 1, current, list, k, result);
            current.remove(current.size() - 1);
        }
    }
}
