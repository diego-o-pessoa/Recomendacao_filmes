import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n🎥 Bem-vindo(a) ao sistema inteligente de recomendação de filmes!");

        Map<Integer, String> filmes = carregarTitulos();
        Map<Integer, List<String>> generosPorFilme = carregarGeneros();
        Map<Integer, Double> medias = calcularMedias();
        Map<Integer, Integer> anos = carregarAnos();

        System.out.println("\nSegue recomendações de filmes em 3 gêneros diferentes:");
        exibirSugestoesPorGenero("Ação e Aventura", Arrays.asList("Action", "Adventure"), "🎬",
                new String[]{"🔫", "🚗", "🦇"}, generosPorFilme, filmes, medias);
        exibirSugestoesPorGenero("Suspense e Mistério", Arrays.asList("Mystery", "Thriller"), "🕵️‍♂️",
                new String[]{"🌀", "🧠", "🧩"}, generosPorFilme, filmes, medias);
        exibirSugestoesPorGenero("Ficção Científica", List.of("Sci-Fi"), "🚀",
                new String[]{"🤖", "🛸", "👽"}, generosPorFilme, filmes, medias);

        System.out.println("\nAgora me diga suas preferências:");

        System.out.print("1️⃣ Qual gênero você prefere? (Ex: ação, comédia, drama, terror, etc) ");
        String generoUsuario = scanner.nextLine();
        String generoNormalizado = normalizar(generoUsuario);

        System.out.print("2️⃣ Prefere lançamentos 'recentes' ou 'clássicos'? ");
        String estilo = scanner.nextLine().toLowerCase();

        Map<String, String> traducoes = new HashMap<>();
        traducoes.put("acao", "Action");
        traducoes.put("aventura", "Adventure");
        traducoes.put("animacao", "Animation");
        traducoes.put("infantil", "Children");
        traducoes.put("comedia", "Comedy");
        traducoes.put("crime", "Crime");
        traducoes.put("documentario", "Documentary");
        traducoes.put("drama", "Drama");
        traducoes.put("fantasia", "Fantasy");
        traducoes.put("terror", "Horror");
        traducoes.put("musical", "Musical");
        traducoes.put("misterio", "Mystery");
        traducoes.put("romance", "Romance");
        traducoes.put("ficcao cientifica", "Sci-Fi");
        traducoes.put("suspense", "Thriller");
        traducoes.put("guerra", "War");
        traducoes.put("farwest", "Western");

        String genero = traducoes.getOrDefault(generoNormalizado, generoNormalizado);

        List<Integer> recomendados = new ArrayList<>();
        for (int id : filmes.keySet()) {
            List<String> generos = generosPorFilme.getOrDefault(id, new ArrayList<>());
            double nota = medias.getOrDefault(id, 0.0);
            int ano = anos.getOrDefault(id, 1900);

            boolean generoOk = generos.contains(genero);
            boolean estiloOk = estilo.equals("recentes") == (ano >= 2010);

            if (generoOk && estiloOk && nota >= 4.0) {
                recomendados.add(id);
            }
        }

        if (recomendados.isEmpty()) {
            System.out.println("\n😞 Desculpe, não encontramos filmes com essas preferências.");
        } else {
            Collections.shuffle(recomendados);
            System.out.println("\n🎯 Aqui estão suas recomendações:");
            for (int i = 0; i < Math.min(3, recomendados.size()); i++) {
                int id = recomendados.get(i);
                String titulo = filmes.get(id);
                double nota = medias.get(id);
                System.out.printf("⭐ %s – Nota média: %.2f [movieId: %d]%n", titulo, nota, id);
            }
        }

        Map<Integer, double[]> vetores = criarVetores(generosPorFilme, medias);

        System.out.print("\n🎯 Quer recomendações baseadas em um filme específico? Digite o ID do filme (ou 0 para encerrar): ");
        int idReferencia = scanner.nextInt();
        scanner.nextLine();

        if (idReferencia != 0 && filmes.containsKey(idReferencia)) {
            System.out.println("\n📌 Filmes similares a " + filmes.get(idReferencia) + ":");
            List<Integer> similares = knn(idReferencia, vetores, 3);
            for (int simId : similares) {
                System.out.printf("⭐ %s – Nota média: %.2f [movieId: %d]%n",
                        filmes.get(simId), medias.getOrDefault(simId, 0.0), simId);
            }
        } else if (idReferencia != 0) {
            System.out.println("❌ Filme não encontrado.");
        }

        scanner.close();
    }

    public static String normalizar(String texto) {
        texto = texto.toLowerCase();
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("\\p{M}", "");
        texto = texto.replace('ç', 'c');
        return texto;
    }

    private static Map<Integer, String> carregarTitulos() {
        Map<Integer, String> mapa = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("/" + "movies.csv")), StandardCharsets.ISO_8859_1))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(",", 3);
                int id = Integer.parseInt(partes[0]);
                String titulo = partes[1];
                mapa.put(id, titulo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapa;
    }

    private static Map<Integer, List<String>> carregarGeneros() {
        Map<Integer, List<String>> generos = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("/" + "movies.csv")), StandardCharsets.ISO_8859_1))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(",", 3);
                int id = Integer.parseInt(partes[0]);
                String[] generosArr = partes[2].split("\\|");
                generos.put(id, Arrays.asList(generosArr));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return generos;
    }

    private static Map<Integer, Double> calcularMedias() {
        Map<Integer, List<Double>> notas = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("/" + "ratings.csv")), StandardCharsets.ISO_8859_1))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(",");
                int filme = Integer.parseInt(partes[1]);
                double nota = Double.parseDouble(partes[2]);
                notas.putIfAbsent(filme, new ArrayList<>());
                notas.get(filme).add(nota);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<Integer, Double> medias = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> entry : notas.entrySet()) {
            double media = entry.getValue().stream().mapToDouble(d -> d).average().orElse(0.0);
            medias.put(entry.getKey(), media);
        }
        return medias;
    }

    private static Map<Integer, Integer> carregarAnos() {
        Map<Integer, Integer> anos = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("/" + "movies.csv")), StandardCharsets.UTF_8))) {
            br.readLine();
            String linha;
            Pattern patternAno = Pattern.compile("\\((\\d{4})\\)");

            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(",", 3);
                int id = Integer.parseInt(partes[0]);
                String titulo = partes[1];

                Matcher matcher = patternAno.matcher(titulo);
                int ano = 1900;
                if (matcher.find()) {
                    try {
                        ano = Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }

                anos.put(id, ano);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return anos;
    }

    private static void exibirSugestoesPorGenero(String titulo, List<String> generosAlvo, String emojiCat,
                                                 String[] emojis, Map<Integer, List<String>> generosPorFilme,
                                                 Map<Integer, String> filmes, Map<Integer, Double> medias) {
        System.out.println("\n" + emojiCat + " " + titulo);

        List<Integer> candidatos = new ArrayList<>();
        for (int id : generosPorFilme.keySet()) {
            List<String> generos = generosPorFilme.get(id);
            for (String alvo : generosAlvo) {
                if (generos.contains(alvo) && medias.getOrDefault(id, 0.0) >= 4.0) {
                    candidatos.add(id);
                    break;
                }
            }
        }

        Collections.shuffle(candidatos);
        for (int i = 0; i < Math.min(3, candidatos.size()); i++) {
            int id = candidatos.get(i);
            String nome = filmes.getOrDefault(id, "Desconhecido");
            System.out.println(emojis[i % emojis.length] + " " + nome + " – Filme bem avaliado! [movieId: " + id + "]");
        }
    }

    private static List<String> listaGenerosFixos() {
        return Arrays.asList("Action", "Adventure", "Animation", "Children", "Comedy",
                "Crime", "Documentary", "Drama", "Fantasy", "Horror", "Musical",
                "Mystery", "Romance", "Sci-Fi", "Thriller", "War", "Western");
    }

    private static Map<Integer, double[]> criarVetores(Map<Integer, List<String>> generosPorFilme, Map<Integer, Double> medias) {
        List<String> generosFixos = listaGenerosFixos();
        Map<Integer, double[]> vetores = new HashMap<>();

        for (int id : generosPorFilme.keySet()) {
            double[] vetor = new double[generosFixos.size() + 1];
            List<String> generos = generosPorFilme.get(id);

            for (int i = 0; i < generosFixos.size(); i++) {
                vetor[i] = generos.contains(generosFixos.get(i)) ? 1.0 : 0.0;
            }

            vetor[generosFixos.size()] = medias.getOrDefault(id, 0.0);
            vetores.put(id, vetor);
        }
        return vetores;
    }

    private static double distanciaEuclidiana(double[] a, double[] b) {
        double soma = 0;
        for (int i = 0; i < a.length; i++) {
            soma += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(soma);
    }

    private static List<Integer> knn(int filmeId, Map<Integer, double[]> vetores, int k) {
        double[] base = vetores.get(filmeId);
        if (base == null) return Collections.emptyList();

        PriorityQueue<Map.Entry<Integer, Double>> pq = new PriorityQueue<>(
                Comparator.comparingDouble(Map.Entry::getValue));

        for (int id : vetores.keySet()) {
            if (id == filmeId) continue;
            double dist = distanciaEuclidiana(base, vetores.get(id));
            pq.offer(new AbstractMap.SimpleEntry<>(id, dist));
        }

        List<Integer> resultados = new ArrayList<>();
        for (int i = 0; i < k && !pq.isEmpty(); i++) {
            resultados.add(pq.poll().getKey());
        }
        return resultados;
    }
}
