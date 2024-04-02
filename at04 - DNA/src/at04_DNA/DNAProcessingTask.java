package at04_DNA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DNAProcessingTask implements Runnable {
    private final File file;
    private static final Map<Character, Character> complementMap = new HashMap<>();
    static {
        complementMap.put('A', 'T');
        complementMap.put('T', 'A');
        complementMap.put('C', 'G');
        complementMap.put('G', 'C');
    }

    private static final AtomicInteger totalFitas = new AtomicInteger(0);
    private static final AtomicInteger validas = new AtomicInteger(0);
    private static final AtomicInteger invalidas = new AtomicInteger(0);
    private static final List<Integer> linhasInvalidas = Collections.synchronizedList(new ArrayList<>());

    public DNAProcessingTask(File file) {
        this.file = file;
    }

    private void writeComplementaryFitasToFile(String fileName, List<String> complementaryFitas) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String fita : complementaryFitas) {
                writer.write(fita);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        List<String> complementaryFitas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int numeroLinha = 1;
            while ((line = reader.readLine()) != null) {
                totalFitas.incrementAndGet();
                String complement = getComplement(line);
                if (complement != null) {
                    validas.incrementAndGet();
                    complementaryFitas.add(complement);
                } else {
                    invalidas.incrementAndGet();
                    synchronized (linhasInvalidas) {
                        linhasInvalidas.add(numeroLinha);
                    }
                    // Adiciona a fita original com o prefixo ”****FITA INVALIDA - ”
                    complementaryFitas.add("****FITA INVALIDA - " + line);
                    System.out.println("Fita inválida em " + file.getName() + ": " + line);
                }
                numeroLinha++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Escreve as fitas complementares em um novo arquivo
        writeComplementaryFitasToFile("arquivosDNAComplementary\\complementaryFitas_" + file.getName(), complementaryFitas);
    }

    private String getComplement(String dna) {
        StringBuilder complement = new StringBuilder();
        for (char nucleotide : dna.toCharArray()) {
            Character complementNucleotide = complementMap.get(nucleotide);
            if (complementNucleotide == null) {
                return null; // Fita inválida
            }
            complement.append(complementNucleotide);
        }
        return complement.toString();
    }

    public static void main(String[] args) {
        String diretorio = "C:\\Users\\jvsan\\OneDrive\\Área de Trabalho\\7º Semestre\\ProcessAltoDesempenho\\at04 - DNA\\arquivosDNA";
        File dir = new File(diretorio);
        if (dir.exists() && dir.isDirectory()) {
            File[] arquivos = dir.listFiles();
            ExecutorService executorService = Executors.newCachedThreadPool();

            for (File arquivo : arquivos) {
                if (arquivo.isFile()) {
                    DNAProcessingTask task = new DNAProcessingTask(arquivo);
                    executorService.execute(task);
                }
            }

            executorService.shutdown(); // Desativa novas tarefas de serem submetidas
            try {
                // Aguarda um tempo máximo para as tarefas existentes terminarem
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow(); // Cancela tarefas atualmente em execução
                    // Aguarda um tempo máximo para as tarefas canceladas responderem
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("O pool não terminou");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancela se a thread atual também foi interrompida
                executorService.shutdownNow();
                // Preserva o status de interrupção
                Thread.currentThread().interrupt();
            }


            System.out.println("Total de fitas: " + totalFitas.get());
            System.out.println("Número de fitas válidas: " + validas.get());
            System.out.println("Número de fitas inválidas: " + invalidas.get());
            System.out.println("Linhas inválidas são: " + linhasInvalidas.toString());
        }
    }
}
