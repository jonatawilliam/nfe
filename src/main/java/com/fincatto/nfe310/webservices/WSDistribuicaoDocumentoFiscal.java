package com.fincatto.nfe310.webservices;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fincatto.dfe.classes.DFUnidadeFederativa;
import com.fincatto.nfe310.NFeConfig;
import com.fincatto.nfe310.classes.NFAutorizador31;
import com.fincatto.nfe310.converters.ElementStringConverter;

import br.inf.portalfiscal.nfe.DistDFeInt;
import br.inf.portalfiscal.nfe.RetDistDFeInt;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.an.NFeDistribuicaoDFe;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.an.NFeDistribuicaoDFeSoap;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.an.NfeDistDFeInteresse;
import br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.an.NfeDistDFeInteresseResponse;

class WSDistribuicaoDocumentoFiscal {

    private static final BigDecimal VERSAO_LEIAUTE = new BigDecimal("1.01");
    private final NFeConfig config;

    WSDistribuicaoDocumentoFiscal(final NFeConfig config) {
        this.config = config;
    }

    RetDistDFeInt pedidoDistribuicao(final String cnpj, final String ultNSU, final DFUnidadeFederativa unidadeFederativa) throws Exception {
        return efetuaConsultaDocumentoFiscal(gerarDadosConsulta(cnpj, "", "", ultNSU, unidadeFederativa), unidadeFederativa);
    }

    RetDistDFeInt pedidoDistribuicaoNSU(final String cnpj, final String nsu, final DFUnidadeFederativa unidadeFederativa) throws Exception {
        return efetuaConsultaDocumentoFiscal(gerarDadosConsulta(cnpj, "", nsu, "", unidadeFederativa), unidadeFederativa);
    }

    RetDistDFeInt pedidoDistribuicaoChave(final String cnpj, final String chave, final DFUnidadeFederativa unidadeFederativa) throws Exception {
        return efetuaConsultaDocumentoFiscal(gerarDadosConsulta(cnpj, chave, "", "", unidadeFederativa), unidadeFederativa);
    }

    private DistDFeInt gerarDadosConsulta(final String cnpj, final String chave, final String nsu, final String ultNSU, final DFUnidadeFederativa unidadeFederativa) {
        final DistDFeInt distDFeInt = new DistDFeInt();
        /**
         * Segundo a NT2014.002_v1.02_WsNFeDistribuicaoDFe a partir de 09/01/2017 este campo será opcional
         */
        distDFeInt.setCUFAutor(unidadeFederativa.getCodigoIbge()); //
        distDFeInt.setTpAmb(this.config.getAmbiente().getCodigo());
        distDFeInt.setVersao(VERSAO_LEIAUTE.toPlainString());
        distDFeInt.setCNPJ(cnpj);
        if (!nsu.isEmpty()) {
            DistDFeInt.ConsNSU consNSU = new DistDFeInt.ConsNSU();
            consNSU.setNSU(nsu);
            distDFeInt.setConsNSU(consNSU);
        }
        if (!chave.isEmpty()) {
            DistDFeInt.ConsChNFe consChave = new DistDFeInt.ConsChNFe();
            consChave.setChNFe(chave);
            distDFeInt.setConsChNFe(consChave);
        }
        if (!ultNSU.isEmpty()) {
            DistDFeInt.DistNSU distNSU = new DistDFeInt.DistNSU();
            distNSU.setUltNSU(ultNSU);
            distDFeInt.setDistNSU(distNSU);
        }
        return distDFeInt;
    }

    private RetDistDFeInt efetuaConsultaDocumentoFiscal(final DistDFeInt distDFeInt, final DFUnidadeFederativa unidadeFederativa) throws RemoteException, JAXBException, MalformedURLException {
        final NFAutorizador31 autorizador = NFAutorizador31.valueOfCodigoUF(unidadeFederativa);
        final String endpoint = autorizador.getNFeDistribuicaoDFe(this.config.getAmbiente());
        if (endpoint == null) {
            throw new IllegalArgumentException("Nao foi possivel encontrar URL para DistribuicaoDocumentoFiscal NF-e, autorizador " + autorizador.name() + ", UF " + unidadeFederativa.name());
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(DistDFeInt.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        JAXBElement<DistDFeInt> jaxbElement = new JAXBElement<>(new QName("http://www.portalfiscal.inf.br/nfe", "distDFeInt"), DistDFeInt.class, distDFeInt);

        DOMResult dOMResult = new DOMResult();
        jaxbMarshaller.marshal(jaxbElement, dOMResult);

        NfeDistDFeInteresse.NfeDadosMsg nfeDadosMsg = new br.inf.portalfiscal.nfe.wsdl.nfedistribuicaodfe.an.ObjectFactory().createNfeDistDFeInteresseNfeDadosMsg();
        nfeDadosMsg.getContent().add(((Document) dOMResult.getNode()).getDocumentElement());

        NFeDistribuicaoDFeSoap port = new NFeDistribuicaoDFe(new URL(endpoint)).getNFeDistribuicaoDFeSoap12();
        NfeDistDFeInteresseResponse.NfeDistDFeInteresseResult result = port.nfeDistDFeInteresse(nfeDadosMsg);

        JAXBContext context = JAXBContext.newInstance("br.inf.portalfiscal.nfe");
        Unmarshaller unmarshaller = context.createUnmarshaller();

        return (RetDistDFeInt) unmarshaller.unmarshal(new StringReader(ElementStringConverter.write((Element) result.getContent().get(0))));
    }

}