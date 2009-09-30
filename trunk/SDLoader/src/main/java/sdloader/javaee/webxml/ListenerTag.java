/*
 * Copyright 2005-2009 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader.javaee.webxml;

/**
 * listenerタグ
 * 
 * @author c9katayama
 * @author shot
 */
public class ListenerTag implements WebXmlTagElement {

	private String listenerClass;

	public String getListenerClass() {
		return listenerClass;
	}

	public ListenerTag setListenerClass(String listenerClass) {
		this.listenerClass = listenerClass;
		return this;
	}

	public void accept(WebXmlVisitor visitor) {
		visitor.visit(this);
	}
}
