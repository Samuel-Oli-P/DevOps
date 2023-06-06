package testLog;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import persistence.API;
import persistence.Conexao;
import persistence.MaquinaService;
import persistence.LogService;
import persistence.LogService;
import persistence.MaquinaService;
import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.discos.DiscoGrupo;
import com.github.britooo.looca.api.group.janelas.Janela;
import com.github.britooo.looca.api.group.janelas.JanelaGrupo;
import com.github.britooo.looca.api.group.rede.Rede;
import com.github.britooo.looca.api.group.rede.RedeInterface;
import com.github.britooo.looca.api.group.sistema.Sistema;
import com.github.britooo.looca.api.util.Conversor;
import java.io.IOException;
import persistence.API;
import persistence.Conexao;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JOptionPane;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import persistence.Dados;
import persistence.FuncionarioService;
import persistence.Log;
import persistence.Maquina;
import persistence.RedeService;
import persistence.Redes;

public class Teste {

    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);
        Scanner leitorLn = new Scanner(System.in);

        System.out.println("Digite seu login:");
        String login = leitor.nextLine();

        System.out.println("Digite sua senha:");
        String senha = leitorLn.nextLine();

        FuncionarioService funcDao = new FuncionarioService();
        API api = new API();
        
        if (!funcDao.login(login, senha).isEmpty()) {
            Looca looca = new Looca();
            MaquinaService maquinaService = new MaquinaService();
            RedeService redeDao = new RedeService();
            Rede rede = looca.getRede();

            Double frequenciaCpu = Double.valueOf(api.getProcessador().getFrequencia());
            frequenciaCpu = frequenciaCpu / 1000000000.00;

            Double capRam = Double.valueOf(api.getMemoria().getTotal());
            capRam = capRam / 1073741824.00;

            Double capDisco = Double.valueOf(api.getDisco().get(0).getTamanho());
            capDisco = capDisco / 1073741824.00;

            Double leituraDisco = Double.valueOf(api.getDisco().get(0).getBytesDeLeitura());
            leituraDisco = leituraDisco / 100000000.00;

            Double escritaDisco = Double.valueOf(api.getDisco().get(0).getBytesDeEscritas());
            escritaDisco = escritaDisco / 100000000.00;

            List<Maquina> hostname = maquinaService.buscarPeloHostname(rede.getParametros().getHostName());
            List<RedeInterface> redes = new ArrayList();
            if (hostname.isEmpty()) {

                for (int i = 0; i < rede.getGrupoDeInterfaces().getInterfaces().size(); i++) {

                    if (!rede.getGrupoDeInterfaces().getInterfaces().get(i).getEnderecoIpv4().isEmpty() && rede.getGrupoDeInterfaces().getInterfaces().get(i).getPacotesRecebidos() > 0 && rede.getGrupoDeInterfaces().getInterfaces().get(i).getPacotesEnviados() > 0) {

                        redes.add(rede.getGrupoDeInterfaces().getInterfaces().get(i));

                    }
                }

                Maquina maquina = new Maquina(null, rede.getParametros().getHostName(), 1, api.getProcessador().getNome(), frequenciaCpu, "Memoria", capRam, api.getDisco().get(0).getModelo(), capDisco, leituraDisco, escritaDisco, funcDao.retornarFkEmpresa(login, senha), 1);

                maquinaService.salvarMaquina(maquina);

                hostname = maquinaService.buscarPeloHostname(rede.getParametros().getHostName());
                System.out.println("Hostname do for do lgin: " + hostname);
                Redes redesCadastrar = new Redes(null, redes.get(0).getNome(), redes.get(0).getNomeExibicao(), redes.get(0).getEnderecoIpv4().get(0), redes.get(0).getEnderecoMac(), hostname.get(0).getIdMaquina());
                redeDao.cadastrarRede(redesCadastrar);
            } else {

                System.out.println("Maquina Ja cadastrada ou houve algum erro");
            }

            hostname = maquinaService.buscarPeloHostname(rede.getParametros().getHostName());
            System.out.println(hostname.get(0).getIdMaquina());

        } else {
            System.out.println("""
                             Senha ou login invalido
                              ou usuario nao cadastrado via web""");
        }

        LogService logService = new LogService();
        MaquinaService maquinaService = new MaquinaService();
        Looca looca = new Looca();

        Rede rede = looca.getRede();
        JanelaGrupo janelaGrupo = looca.getGrupoDeJanelas();
        DiscoGrupo disco = looca.getGrupoDeDiscos();

        List<Maquina> hostname = maquinaService.buscarPeloHostname(rede.getParametros().getHostName());

        Double usoDisco = (double) (api.getDisco().get(0).getTamanho() - disco.getVolumes().get(0).getDisponivel());
        usoDisco = usoDisco / disco.getDiscos().get(0).getTamanho() * 100;
        System.out.println(usoDisco);

        Double usoRam = Double.valueOf(api.getMemoriaEmUso());
        usoRam = usoRam / looca.getMemoria().getTotal() * 100;
        System.out.println(usoRam);

        Double finalUsoDisco = usoDisco;
        Double finalUsoRam = usoRam;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<String> janelas = new ArrayList<>();
                List<Long> janelasPid = new ArrayList<>();

                for (int i = 0; i < janelaGrupo.getTotalJanelasVisiveis(); i++) {
                    if (janelaGrupo.getJanelasVisiveis().get(i).getTitulo().length() > 0) {
                        janelas.add(janelaGrupo.getJanelasVisiveis().get(i).getTitulo());
                        janelasPid.add(janelaGrupo.getJanelasVisiveis().get(i).getPid());
                    }
                }
                List<RedeInterface> redes = new ArrayList<>();

                for (int i = 0; i < rede.getGrupoDeInterfaces().getInterfaces().size(); i++) {

                    if (!rede.getGrupoDeInterfaces().getInterfaces().get(i).getEnderecoIpv4().isEmpty() && rede.getGrupoDeInterfaces().getInterfaces().get(i).getPacotesRecebidos() > 0 && rede.getGrupoDeInterfaces().getInterfaces().get(i).getPacotesEnviados() > 0) {

                        redes.add(rede.getGrupoDeInterfaces().getInterfaces().get(i));
                        break;

                    }
                }

                for (int j = 0; j < janelas.size(); j++) {
                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                    Log log = new Log(null, timeStamp, janelasPid.get(j), janelas.get(j), api.getProcessador().getUso(), finalUsoDisco, finalUsoRam, (redes.get(0).getBytesRecebidos() * 8) / 1000000, (redes.get(0).getBytesEnviados() * 8) / 1000000, hostname.get(0).getIdMaquina());
                    System.out.println(log);
                    logService.salvarLog(log);
                }
            }
        }, 0, 3000);

    }

}
