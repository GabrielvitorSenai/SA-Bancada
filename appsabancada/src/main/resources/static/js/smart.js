// Estado compartilhado da interface de operação

let eventSources = {}; // Canais SSE por estação

const clps = ["clp1", "clp2", "clp3", "clp4"];
let clpEventSources = {}; // Mantém as conexões abertas com os CLPs
let conectado = false;
let pausado = -1;

let statusEstoque = 0;
let statusProcesso = 0;
let statusMontagem = 0;
let statusExpedicao = 0;
let statusProducao = 0;
let pedidoEmCurso = 0;

console.info("appsabancada iniciado");

// Preparação inicial da interface
document.addEventListener("DOMContentLoaded", function () {
    if (sessionStorage.getItem("bancadaConectada") === "true") {
        document.getElementById("btnConectar").textContent = "Desconectar";
        conectado = true;
        iniciarSSEClps();
    }
});

document.addEventListener("DOMContentLoaded", function () {
    const inputPrincipal = document.getElementById("hostIp");
    //restaurarValores();

    inputPrincipal.addEventListener("blur", function () {
        const baseIp = inputPrincipal.value.trim();
        if (baseIp) {
            const partes = baseIp.split(".");
            if (partes.length === 4) {
                const base = partes.slice(0, 3).join(".");
                document.getElementById("hostIpEstoque").value = `${base}.10`;
                document.getElementById("hostIpProcesso").value = `${base}.20`;
                document.getElementById("hostIpMontagem").value = `${base}.30`;
                document.getElementById("hostIpExpedicao").value = `${base}.40`;
                salvarValores();
            }
        }
    });

    // Reaplica preferências visuais armazenadas
    ["Estoque", "Processo", "Montagem", "Expedicao"].forEach(nome => {
        const inputId = `hostIp${nome}`;
        const input = document.getElementById(inputId);
        const corSalva = sessionStorage.getItem(`corFonte_${inputId}`);
        if (corSalva) input.style.color = corSalva;
    });

    // Recarrega as últimas leituras exibidas
    ["Estoque", "Processo", "Montagem", "Expedicao"].forEach(nome => {
        const leitura = sessionStorage.getItem(`leitura${nome}`);
        if (leitura) {
            document.getElementById(`leitura${nome}`).value = leitura;
        }
    });

    // Recompõe o estado de conexão salvo na sessão
    const btn = document.getElementById("btnConectar");
    if (sessionStorage.getItem("bancadaConectada") === "true") {
        btn.textContent = "Desconectar";
        conectado = true;
        iniciarSSEClps(); // Reabre os fluxos de leitura
    } else {
        btn.textContent = "Conectar";
        conectado = false;
    }

    document.querySelectorAll("input[type='text']").forEach(input => {
        input.addEventListener("blur", salvarValores);
    });
});

function salvarValores() {
    sessionStorage.setItem("hostIp", document.getElementById("hostIp").value);
    sessionStorage.setItem("hostIpEstoque", document.getElementById("hostIpEstoque").value);
    sessionStorage.setItem("hostIpProcesso", document.getElementById("hostIpProcesso").value);
    sessionStorage.setItem("hostIpMontagem", document.getElementById("hostIpMontagem").value);
    sessionStorage.setItem("hostIpExpedicao", document.getElementById("hostIpExpedicao").value);
}

function restaurarValores() {
    document.getElementById("hostIp").value = sessionStorage.getItem("hostIp") || "";
    document.getElementById("hostIpEstoque").value = sessionStorage.getItem("hostIpEstoque") || "";
    document.getElementById("hostIpProcesso").value = sessionStorage.getItem("hostIpProcesso") || "";
    document.getElementById("hostIpMontagem").value = sessionStorage.getItem("hostIpMontagem") || "";
    document.getElementById("hostIpExpedicao").value = sessionStorage.getItem("hostIpExpedicao") || "";
}

function processarDadosClp(clp, data) {
    try {

        // Exibe o pacote bruto recebido para conferência
        const display = document.getElementById(`${clp}-dados`);
        if (display) display.textContent = data;

        const hexArray = data.trim().split(/\s+/);
        const byteArray = hexArray.map(hex => parseInt(hex, 16));



        if (isNaN(byteArray[0])) {
            console.warn(`Dados inválidos recebidos de ${clp}:`, data);
            return;
        }

        // Conversão de bytes em ponto flutuante
        function bytesToFloat32(byteArr) {
            const buf = new ArrayBuffer(4);
            const view = new DataView(buf);
            byteArr.forEach((b, i) => view.setUint8(i, b));
            return view.getFloat32(0, false); // little-endian
        }

        // Tratamento dos dados da estação de estoque
        if (clp === "clp1") {
            for (let i = 0; i < 28; i++) {
                const corValor = byteArray[68 + i];
                const celula = document.getElementById(`estoque-${i + 1}`);
                if (celula) {
                    const corFundo = getCor(corValor);
                    const corFonte = getCorFonte(corFundo);
                    celula.style.backgroundColor = corFundo;
                    celula.style.color = corFonte;
                }
            }
            const arrayColorEstoque = byteArray.slice(143, 143 + 28);
            const arrayEixoVerticalValue = byteArray.slice(117, 117 + 4);
            const arrayEixoRotativoValue = byteArray.slice(121, 121 + 4);

            // Conversão dos valores de eixo
            const eixoVertical = bytesToFloat32(arrayEixoVerticalValue);
            const eixoRotativo = bytesToFloat32(arrayEixoRotativoValue);

            // Preenchimento dos campos de leitura na tela
            document.getElementById('atuadorLinearEst').value = eixoVertical.toFixed(2);
            document.getElementById('atuadorRotativoEst').value = eixoRotativo.toFixed(2);

            applyColors(arrayColorEstoque);


            iniciarPedido = (byteArray[62] & 0b00000001) !== 0;
            startOpEst = (byteArray[98] & 0b00000100) !== 0;

            // if (iniciarPedido && startOpEst) {
            //     pedidoEmCurso = true;
            // }

            //if(pedidoEmCurso === true){
            statusEstoque = byteArray[171];
            statusProcesso = byteArray[172];
            statusMontagem = byteArray[173];
            statusExpedicao = byteArray[174];
            statusProducao = byteArray[175];
            pedidoEmCurso = byteArray[176];
            //}

            // Aí você dispara/para o contador:
            // if ((statusProducao === 0 && pedidoEmCurso === 1) && !contadorInterval) {
            //     iniciarContador();
            // }

            if (statusEstoque === 1 && statusProcesso === 0 && statusMontagem === 0 && statusExpedicao === 0) {
                pedidoEmCurso = true;
                iniciarContador();
            }

            // Variáveis booleanas
            atualizarCampoBooleano("var1-1", (byteArray[0] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-5", (byteArray[62] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-6", (byteArray[64] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-7", (byteArray[64] & 0b00000010) !== 0);
            atualizarCampoBooleano("var1-11", (byteArray[98] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-12", (byteArray[98] & 0b00000010) !== 0);
            atualizarCampoBooleano("var1-10", (byteArray[98] & 0b00000100) !== 0);
            atualizarCampoBooleano("var1-13", (byteArray[100] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-20", (byteArray[100] & 0b00000010) !== 0);
            atualizarCampoBooleano("var1-21", (byteArray[100] & 0b00000100) !== 0);
            atualizarCampoBooleano("var1-22", (byteArray[100] & 0b00001000) !== 0);
            atualizarCampoBooleano("var1-14", (byteArray[102] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-16", (byteArray[106] & 0b00000001) !== 0);
            atualizarCampoBooleano("var1-17", (byteArray[106] & 0b00000010) !== 0);
            atualizarCampoBooleano("var1-18", (byteArray[106] & 0b00000100) !== 0);

            if((byteArray[115] & 0b00100000) !== 0){
                document.querySelector('input[name="esteira1Est"]').value = "LIGADO";
            }else{
                document.querySelector('input[name="esteira1Est"]').value = "DESLIGADO";
            }

            if((byteArray[115] & 0b01000000) !== 0){
                document.querySelector('input[name="esteira2Est"]').value = "LIGADO";
            }else{
                document.querySelector('input[name="esteira2Est"]').value = "DESLIGADO";
            }

            // Variáveis inteiras

            // Andar 1
            document.querySelector('input[name="corBloco1"]').value = (byteArray[2] << 8) | byteArray[3];
            document.querySelector('input[name="posicaoBloco1"]').value = (byteArray[4] << 8) | byteArray[5];
            document.querySelector('input[name="andar1-cor1"]').value = (byteArray[6] << 8) | byteArray[7];
            document.querySelector('input[name="andar1-cor2"]').value = (byteArray[8] << 8) | byteArray[9];
            document.querySelector('input[name="andar1-cor3"]').value = (byteArray[10] << 8) | byteArray[11];
            document.querySelector('input[name="andar1-padrao1"]').value = (byteArray[12] << 8) | byteArray[13];
            document.querySelector('input[name="andar1-padrao2"]').value = (byteArray[14] << 8) | byteArray[15];
            document.querySelector('input[name="andar1-padrao3"]').value = (byteArray[16] << 8) | byteArray[17];
            document.querySelector('input[name="procAndar1"]').value = (byteArray[18] << 8) | byteArray[19];

            // Andar 2
            document.querySelector('input[name="corBloco2"]').value = (byteArray[20] << 8) | byteArray[21];
            document.querySelector('input[name="posicaoBloco2"]').value = (byteArray[22] << 8) | byteArray[23];
            document.querySelector('input[name="andar2-cor1"]').value = (byteArray[24] << 8) | byteArray[25];
            document.querySelector('input[name="andar2-cor2"]').value = (byteArray[26] << 8) | byteArray[27];
            document.querySelector('input[name="andar2-cor3"]').value = (byteArray[28] << 8) | byteArray[29];
            document.querySelector('input[name="andar2-padrao1"]').value = (byteArray[30] << 8) | byteArray[31];
            document.querySelector('input[name="andar2-padrao2"]').value = (byteArray[32] << 8) | byteArray[33];
            document.querySelector('input[name="andar2-padrao3"]').value = (byteArray[34] << 8) | byteArray[35];
            document.querySelector('input[name="procAndar2"]').value = (byteArray[36] << 8) | byteArray[37];

            // Andar 3
            document.querySelector('input[name="corBloco3"]').value = (byteArray[38] << 8) | byteArray[39];
            document.querySelector('input[name="posicaoBloco3"]').value = (byteArray[40] << 8) | byteArray[41];
            document.querySelector('input[name="andar3-cor1"]').value = (byteArray[42] << 8) | byteArray[43];
            document.querySelector('input[name="andar3-cor2"]').value = (byteArray[44] << 8) | byteArray[45];
            document.querySelector('input[name="andar3-cor3"]').value = (byteArray[46] << 8) | byteArray[47];
            document.querySelector('input[name="andar3-padrao1"]').value = (byteArray[48] << 8) | byteArray[49];
            document.querySelector('input[name="andar3-padrao2"]').value = (byteArray[50] << 8) | byteArray[51];
            document.querySelector('input[name="andar3-padrao3"]').value = (byteArray[52] << 8) | byteArray[53];
            document.querySelector('input[name="procAndar3"]').value = (byteArray[54] << 8) | byteArray[55];


            document.querySelector('input[name="var1-2"]').value = (byteArray[56] << 8) | byteArray[57];
            document.querySelector('input[name="var1-3"]').value = (byteArray[58] << 8) | byteArray[59];
            document.querySelector('input[name="var1-4"]').value = (byteArray[60] << 8) | byteArray[61];
            document.querySelector('input[name="var1-8"]').value = (byteArray[66] << 8) | byteArray[67];
            document.querySelector('input[name="var1-9"]').value = (byteArray[96] << 8) | byteArray[97];
            document.querySelector('input[name="var1-15"]').value = (byteArray[104] << 8) | byteArray[105];
            document.querySelector('input[name="var1-19"]').value = (byteArray[108] << 8) | byteArray[109];

            if (conectado) {
                document.getElementById("bancada_Estoque").src = "assets/bancada/Smart40_Estoque_" + statusEstoque + ".png";
                document.getElementById("bancada_Processo").src = "assets/bancada/Smart40_Processo_" + statusProcesso + ".png";
                document.getElementById("bancada_Montagem").src = "assets/bancada/Smart40_Montagem_" + statusMontagem + ".png";
                document.getElementById("bancada_Expedicao").src = "assets/bancada/Smart40_Expedicao_" + statusExpedicao + ".png";
            }
        }

        // CLP2 - Processo
        else if (clp === "clp2") {

            atualizarCampoBooleano("var2-1", (byteArray[0] & 0b00000001) !== 0);
            atualizarCampoBooleano("var2-4", (byteArray[4] & 0b00000001) !== 0);
            atualizarCampoBooleano("var2-5", (byteArray[4] & 0b00000010) !== 0);
            atualizarCampoBooleano("var2-3", (byteArray[4] & 0b00000100) !== 0);
            atualizarCampoBooleano("var2-6", (byteArray[6] & 0b00000001) !== 0);
            atualizarCampoBooleano("var2-7", (byteArray[6] & 0b00000010) !== 0);
            atualizarCampoBooleano("var2-8", (byteArray[6] & 0b00000100) !== 0);
            atualizarCampoBooleano("var2-9", (byteArray[6] & 0b00001000) !== 0);
            document.querySelector('input[name="var2-2"]').value = (byteArray[2] << 8) | byteArray[3];
        }

        // CLP3 - Montagem
        else if (clp === "clp3") {



            atualizarCampoBooleano("var3-1", (byteArray[0] & 0b00000001) !== 0);
            atualizarCampoBooleano("var3-4", (byteArray[4] & 0b00000001) !== 0);
            atualizarCampoBooleano("var3-5", (byteArray[4] & 0b00000010) !== 0);
            atualizarCampoBooleano("var3-3", (byteArray[4] & 0b00000100) !== 0);
            atualizarCampoBooleano("var3-6", (byteArray[6] & 0b00000001) !== 0);

            atualizarCampoBooleano("var3-7", (byteArray[6] & 0b00000010) !== 0);
            atualizarCampoBooleano("var3-8", (byteArray[6] & 0b00000100) !== 0);
            atualizarCampoBooleano("var3-9", (byteArray[6] & 0b00001000) !== 0);


            document.querySelector('input[name="var3-2"]').value = (byteArray[2] << 8) | byteArray[3];

            // Extrair status das bancadas
            function extrairStatus(inicio) {
                const tamanhoReal = byteArray[inicio + 1];
                const bytesTexto = byteArray.slice(inicio + 2, inicio + 2 + tamanhoReal);
                return new TextDecoder().decode(new Uint8Array(bytesTexto));
            }

            let statusStrEstoque = extrairStatus(9);     // byte[9] ~ byte[24]
            let statusStrProcesso = extrairStatus(25);   // byte[25] ~ byte[40]
            let statusStrMontagem = extrairStatus(41);   // byte[41] ~ byte[56]
            let statusStrExpedicao = extrairStatus(57);  // byte[57] ~ byte[72]

            if (statusEstoque === 1) {
                statusStrEstoque = "PRODUZINDO";
            } else {
                statusStrEstoque = extrairStatus(9);     // byte[9] ~ byte[24]
            }

            if (statusProcesso === 1) {
                statusStrProcesso = "PRODUZINDO";
            } else {
                statusStrProcesso = extrairStatus(25);   // byte[25] ~ byte[40]
            }

            if (statusMontagem === 0 && statusStrMontagem === "PRODUZINDO") {

                statusStrMontagem = "ROBÔ EM OPERAÇÃO";
                statusMontagem = 1;

            } else if (statusMontagem === 1 && statusStrMontagem === "LIVRE") {
                statusStrMontagem = "ROBÔ EM OPERAÇÃO";
            } else {
                statusStrMontagem = extrairStatus(41);   // byte[41] ~ byte[56]
            }

            if (statusExpedicao === 1) {
                statusStrExpedicao = "PRODUZINDO";
            }
            else {
                statusStrExpedicao = extrairStatus(57);  // byte[57] ~ byte[72]
            }

            // Exemplo: exibir no console ou atualizar campos HTML
            console.log("Estoque:", statusStrEstoque);
            console.log("Processo:", statusStrProcesso);
            console.log("Montagem:", statusStrMontagem);
            console.log("Expedição:", statusStrExpedicao);

            // Você também pode atualizar campos na interface se desejar
            document.querySelector('input[name="var1-statusEstoque"]').value = statusStrEstoque;
            document.querySelector('input[name="var2-statusProcesso"]').value = statusStrProcesso;
            document.querySelector('input[name="var3-statusMontagem"]').value = statusStrMontagem;
            document.querySelector('input[name="var4-statusExpedicao"]').value = statusStrExpedicao;

            document.getElementById("statusOpEstoque").value = statusStrEstoque;
            document.getElementById("statusOpProcesso").value = statusStrProcesso;
            document.getElementById("statusOpMontagem").value = statusStrMontagem;
            document.getElementById("statusOpExpedicao").value = statusStrExpedicao;

            // Função para aplicar cor conforme o status
            function aplicarCorStatus(idInput, status) {
                const el = document.getElementById(idInput);
                if (!el) return;

                switch (status.toUpperCase()) {
                    case "DESLIGADO":
                        el.style.color = 'rgb(255, 0, 0)';
                        break;
                    case "LIVRE":
                        el.style.color = 'rgb(0, 255, 0)';
                        break;
                    case "PRODUZINDO":
                        el.style.color = 'rgb(255, 255, 0)';
                        break;
                    case "ROBÔ EM OPERAÇÃO":
                        el.style.color = 'rgb(255, 255, 0)';
                        break;
                    case "EMERGENCIA":
                        el.style.color = 'rgb(255, 155, 0)';
                        break;
                    default:
                        el.style.color = "black"; // Fallback
                }
            }

            // Aplica cores nos 4 campos de status
            aplicarCorStatus("statusOpEstoque", statusStrEstoque);
            aplicarCorStatus("statusOpProcesso", statusStrProcesso);
            aplicarCorStatus("statusOpMontagem", statusStrMontagem);
            aplicarCorStatus("statusOpExpedicao", statusStrExpedicao);


        }

        // CLP4 - Expedição
        else if (clp === "clp4") {
            for (let i = 0; i < 12; i++) {
                const valorInt = (byteArray[6 + i * 2] << 8) | byteArray[6 + i * 2 + 1];
                const celula = document.getElementById(`expedicao-${i + 1}`);
                if (celula) {
                    celula.textContent = valorInt;
                    celula.style.backgroundColor = valorInt === 0 ? "#ccffcc" : "#ffcccc";
                    celula.style.color = "black";
                }
            }


            recebidoOpExp = (byteArray[0] & 0b00000001) !== 0;
            finishOpExp = (byteArray[32] & 0b00000010) !== 0;

            if (recebidoOpExp && finishOpExp) {
                pedidoConcluido = true;
            }
            // Aí você dispara/para o contador:
            // if ((statusProducao === 1 && pedidoEmCurso === 0) && contadorInterval) {
            //     pararContador();
            // }

            if (statusEstoque == 2 && statusProcesso == 2 && statusMontagem == 2 && statusExpedicao == 2) {
                pedidoEmCurso = false;
                pararContador();
                document.getElementById("btnExecutarPedidoProducao").textContent = "Pedido concluido";
            }



            atualizarCampoBooleano("var4-1", (byteArray[0] & 0b00000001) !== 0);
            atualizarCampoBooleano("var4-2", (byteArray[2] & 0b00000001) !== 0);
            atualizarCampoBooleano("var4-3", (byteArray[2] & 0b00000010) !== 0);
            atualizarCampoBooleano("var4-7", (byteArray[32] & 0b00000001) !== 0);
            atualizarCampoBooleano("var4-8", (byteArray[32] & 0b00000010) !== 0);
            atualizarCampoBooleano("var4-6", (byteArray[32] & 0b00000100) !== 0);
            atualizarCampoBooleano("var4-9", (byteArray[34] & 0b00000001) !== 0);
            atualizarCampoBooleano("var4-10", (byteArray[36] & 0b00000001) !== 0);
            atualizarCampoBooleano("var4-13", (byteArray[42] & 0b00000001) !== 0);
            atualizarCampoBooleano("var4-14", (byteArray[42] & 0b00000010) !== 0);

            atualizarCampoBooleano("var4-16", (byteArray[34] & 0b00000010) !== 0);
            atualizarCampoBooleano("var4-17", (byteArray[34] & 0b00000100) !== 0);
            atualizarCampoBooleano("var4-18", (byteArray[34] & 0b00001000) !== 0);


            document.querySelector('input[name="var4-4"]').value = (byteArray[4] << 8) | byteArray[5];
            document.querySelector('input[name="var4-5"]').value = (byteArray[30] << 8) | byteArray[31];
            document.querySelector('input[name="var4-11"]').value = (byteArray[38] << 8) | byteArray[39];
            document.querySelector('input[name="var4-12"]').value = (byteArray[40] << 8) | byteArray[41];
            document.querySelector('input[name="var4-15"]').value = (byteArray[44] << 8) | byteArray[45];
        }

        if (conectado) {
            document.getElementById("bancada_Estoque").src = "assets/bancada/Smart40_Estoque_" + statusEstoque + ".png";
            document.getElementById("bancada_Processo").src = "assets/bancada/Smart40_Processo_" + statusProcesso + ".png";
            if (statusStrMontagem === "PRODUZINDO" || statusStrMontagem === "ROBÔ EM OPERAÇÃO") {
                statusMontagem = 1;
            }
            document.getElementById("bancada_Montagem").src = "assets/bancada/Smart40_Montagem_" + statusMontagem + ".png";

            document.getElementById("bancada_Expedicao").src = "assets/bancada/Smart40_Expedicao_" + statusExpedicao + ".png";
        }

    } catch (error) {
        console.error(`Erro ao processar dados do ${clp}:`, error, data);
    }
}

// Função para iniciar SSE para os CLPs (usando o mesmo endpoint /smartstream/{bancada})
function iniciarSSEClps() {
    const clpToBancada = {
        "clp1": "estoque",
        "clp2": "processo",
        "clp3": "montagem",
        "clp4": "expedicao"
    };

    clps.forEach(clp => {
        if (clpEventSources[clp]) {
            clpEventSources[clp].close();
        }

        const bancada = clpToBancada[clp];
        const ip = document.getElementById(`hostIp${capitalize(bancada)}`).value;
        const url = `/smartstream/${bancada}?ip=${encodeURIComponent(ip)}`;

        const source = new EventSource(url);
        clpEventSources[clp] = source;

        source.addEventListener("leitura", function (event) {
            console.log(`📡 ${clp.toUpperCase()} => ${event.data}`);

            // Processa os dados do CLP
            processarDadosClp(clp, event.data);

            // Atualiza a leitura correspondente
            const leituraId = `leitura${capitalize(bancada)}`;
            document.getElementById(leituraId).value = `${capitalize(bancada)}: ${event.data}`;
            sessionStorage.setItem(leituraId, event.data);
        });

        source.onerror = function (err) {
            console.error(`Erro SSE [${clp}]:`, err);
            source.close();
            delete clpEventSources[clp];
        };
    });
}

// Função para parar as conexões SSE dos CLPs
function pararSSEClps() {
    clps.forEach(clp => {
        if (clpEventSources[clp]) {
            clpEventSources[clp].close();
            delete clpEventSources[clp];
        }
    });
}

// Função para gerenciar a conexão com as estações de trabalho da Bancada Smart

function conectarBancada() {
    const hostIpInput = document.getElementById('hostIp');

    if (hostIpInput.value.trim() === "") {
        console.log("O endereço de rede da bancada não foi irformado.");
        //alert("O endereço de rede da bancada não foi irformado.");
        // document.getElementById('msgAlert').disabled = false;
        document.getElementById("msgAlert").style.display = "block";
        //document.getElementById('msgAlert').innerText = "O endereço de rede da bancada não foi irformado.";
        return;
    }


    const btn = document.getElementById("btnConectar");
    const ips = {
        estoque: document.getElementById("hostIpEstoque").value,
        processo: document.getElementById("hostIpProcesso").value,
        montagem: document.getElementById("hostIpMontagem").value,
        expedicao: document.getElementById("hostIpExpedicao").value
    };

    let clpsOnline = 0;

    if (!conectado) {
        fetch("/smart/ping", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(ips)
        })
            .then(res => res.json())
            .then(status => {
                Object.entries(status).forEach(([nome, ok]) => {
                    const inputId = `hostIp${capitalize(nome)}`;
                    const input = document.getElementById(inputId);
                    const cor = ok ? "rgb(0,255,0)" : "rgb(255,0,0)";
                    const sts = ok ? "on" : "off";

                    console.log(nome + ": " + ok + " - " + sts);
                    input.style.color = cor;
                    sessionStorage.setItem(`corFonte_${inputId}`, cor);

                    if (ok) clpsOnline++;

                    const imgMap = {
                        estoque: "bancada-est",
                        processo: "bancada-pro",
                        montagem: "bancada-mon",
                        expedicao: "bancada-exp"
                    };

                    const prefixMap = {
                        estoque: "Est",
                        processo: "Pro",
                        montagem: "Mon",
                        expedicao: "Exp"
                    };

                    const imgId = imgMap[nome];
                    const prefix = prefixMap[nome];
                    if (imgId && prefix) {
                        document.getElementById(imgId).src = `assets/bancada/Smart40-${prefix}_${sts}.png`;
                    }

                });

                // Só continua se todos os CLPs estiverem online
                if (clpsOnline === 4) {
                    //----------- Atualiza os dados de Estoque e Expedição nos Clps
                    enviarParaClpEstoque();
                    enviarParaClpExpedicao();

                    //----------- Inicializa as leituras de dados dos Clps
                    fetch("/start-leituras", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify(ips)
                    })
                        .then(() => {
                            iniciarSSEClps(); // Inicia SSE
                            pausado = 0;

                            // Atualiza cor dos inputs
                            document.querySelectorAll('.divBancadaStatus input').forEach(input => {
                                input.style.color = "rgb(0,255,0)";
                            });

                            btn.textContent = "Desconectar";
                            conectado = true;
                            sessionStorage.setItem("bancadaConectada", "true");
                        });
                } else {
                    alert("Nem todos os CLPs responderam. Conexão cancelada.");
                }
            })
            .catch(error => {
                console.error("Erro ao conectar:", error);
            });

    } else {
        // === DESCONECTAR ===
        pararSSEClps();

        if (pausado === 0) pausado = 1;

        if (pausado === 1) {

            document.querySelectorAll('.divBancadaStatus input').forEach(input => {
                input.style.color = "rgb(255,255,0)";
            });


            document.getElementById("bancada-est").src = "assets/bancada/Smart40-Est_pause.png";
            document.getElementById("bancada-pro").src = "assets/bancada/Smart40-Pro_pause.png";
            document.getElementById("bancada-mon").src = "assets/bancada/Smart40-Mon_pause.png";
            document.getElementById("bancada-exp").src = "assets/bancada/Smart40-Exp_pause.png";
        }

        fetch("/stop-leituras", { method: "POST" });

        // clps.forEach(clp => {
        //     document.getElementById(`${clp}-dados`).textContent = "--";
        // });

        // ["Estoque", "Processo", "Montagem", "Expedicao"].forEach(nome => {
        //     document.getElementById(`leitura${nome}`).value = "--";
        // });

        btn.textContent = "Conectar";
        conectado = false;
        sessionStorage.removeItem("bancadaConectada");
    }
}

// Funções auxiliares
function atualizarCampoBooleano(nomeCampo, valor) {
    const campo = document.querySelector(`input[name="${nomeCampo}"]`);
    if (campo) {
        campo.value = String(valor).toUpperCase();
        campo.style.color = "#ffffff";
        campo.style.backgroundColor = valor ? "#00ff00" : "#ff0000";
    }
}

function getCor(valor) {
    switch (valor) {
        case 0: return "white";
        case 1: return "black";
        case 2: return "red";
        case 3: return "blue";
        default: return "gray";
    }
}

function getCorFonte(corFundo) {
    const fundosEscuros = ["black", "blue", "red", "gray"];
    return fundosEscuros.includes(corFundo) ? "white" : "black";
}

function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

window.addEventListener('beforeunload', () => {
    // Armazene o HTML da div dinamicamente construída
    localStorage.setItem('blocosHTML', document.getElementById('blocosContainer').innerHTML);
    localStorage.setItem('tipoPedido', document.getElementById('tipoPedido').value);
});

window.addEventListener('load', () => {
    const blocosHTML = localStorage.getItem('blocosHTML');
    const tipoPedido = localStorage.getItem('tipoPedido');

    if (blocosHTML) {
        document.getElementById('blocosContainer').innerHTML = blocosHTML;
        document.getElementById('tipoPedido').value = tipoPedido;

        // Se precisar reaplicar eventos nos elementos recriados:
        renderBlocos(); // ou algum outro método para reativar lógica JS
    }
});

function fases(fase) {

    if (fase == 1) {

        if (document.getElementById("btnExecutarPedidoProducao").textContent === "Executar Pedido") {
            if (!modoLeitura) {
                iniciarContador();
                executarPedido();
                document.getElementById("btnExecutarPedidoProducao").textContent = "Pedido em curso";
            }

        } else if (document.getElementById("btnExecutarPedidoProducao").textContent === "Pedido concluido") {

            document.getElementById("btnExecutarPedidoProducao").textContent = "Executar Pedido";
            statusEstoque = 0;
            statusProcesso = 0;
            statusMontagem = 0;
            statusExpedicao = 0;
            pedidoEmcurso = false;

            // Chamada ao backend para zerar também os status lá
            fetch('/smart/reset-status', {
                method: 'POST'
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Erro ao resetar status no backend');
                    }
                    return response.text();
                })
                .then(data => {
                    console.log('Backend:', data);
                })
                .catch(error => {
                    console.error('Erro na chamada ao backend:', error);
                });
            if (!modoLeitura) {
                carregarValoresEstoque();
                carregarValoresExpedicao();
            }
        }
    } else if (fase == 2) {

        pararContador();
    } else if (fase == 3) {
        statusMontagem++;
        if (statusMontagem > 2) {
            statusMontagem = 0;
        }

    } else if (fase == 4) {
        statusExpedicao++;
        if (statusExpedicao > 2) {
            statusExpedicao = 0;
        }
    }

}



window.onload = renderBlocos;
