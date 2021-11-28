package life.genny.messages.util;

import java.util.Base64;

public class MsgUtils {
	
	public static String encodeUrl(String base, String parentCode, String code, String targetCode) {
		return encodeUrl(base, parentCode, code, targetCode, null);
	}

	/**
	* A Function for Base64 encoding urls
	*
	* @param base			The base of the url that should not be encoded
	* @param parentCode		the parentCode to encode
	* @param code			The code to encode
	* @param targetCode		the targetCode to encode
	* @param token			The token to attach that should not be encoded
	*
	* @return				The comlpete URL
	 */
	public static String encodeUrl(String base, String parentCode, String code, String targetCode, String token) {
		/**
		 * A Function for Base64 encoding urls
		 **/

		// Encode Parent and Code
		String encodedParentCode = new String(Base64.getEncoder().encode(parentCode.getBytes()));
		String encodedCode = new String(Base64.getEncoder().encode(code.getBytes()));
		String url = base + "/" + encodedParentCode + "/" + encodedCode;

		// Add encoded targetCode if not null
		if (targetCode != null) {
			String encodedTargetCode = new String(Base64.getEncoder().encode(targetCode.getBytes()));
			url = url + "/" + encodedTargetCode;
		}

		// Add access token if not null
		if (token != null) {
			url = url +"?token=" + token;
		}
		return url;
	}

}
