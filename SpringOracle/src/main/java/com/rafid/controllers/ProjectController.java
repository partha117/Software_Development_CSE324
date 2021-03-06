package com.rafid.controllers;

import com.rafid.models.Users;
import com.rafid.project.repositoryDetails;
import com.rafid.repositories.UserRepository;
import com.rafid.util.Constants;
import org.apache.tomcat.jni.Time;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by ASUS on 29-Apr-17.
 */


@Controller
public class ProjectController {
    /**
     * The HelloWorld program implements an application that
     * simply displays "Hello World!" to the standard output.
     *
     *@author (classes and interfaces only, required)
     *@version (classes and interfaces only, required. See footnote 1)
     *@exception IndexOutOfBoundsException if it throws any exception
     *@return arg2  desciption of return
     *@see if any link or message
     *@since starting date
     *@serial (or serialField or serialData)

     */
    @Autowired
    private UserRepository userRepository;
    @RequestMapping("/project")
    public String projectTab(HttpSession httpSession,Model model,@ModelAttribute("Optional")String  attribute,RedirectAttributes redirectAttributes)
    {
        String name=(String)httpSession.getAttribute(Constants.userName);
        model.addAttribute(Constants.name_in_page, httpSession.getAttribute(Constants.user_name).toString());
        if(name==null ||(name.length()==0))
        {
            return "redirect:/login";
        }
        else
        {
            List<Users>user=userRepository.findByUserName(name);
            if((attribute==null)||(attribute.length()==0))
            {
                model.addAttribute("Optional","NotNecessary");
            }
            else
            {
                model.addAttribute("Optional",attribute);
            }

            if(user.size()!=0)
            {
                if((user.get(0).getGitUserId()==null)||user.get(0).getGitUserId().length()==0)
                {
                    return "gitLogin";
                }
                else
                {
                    String gitUserName=(userRepository.findByUserName(name).get(0).getGitUserId());
                    String gitPassword=(userRepository.findByUserName(name).get(0).getGitPassword());
                    GitHub gitHub = null;
                    try {
                        gitHub = GitHub.connectUsingPassword(gitUserName,gitPassword);
                        Map<String, GHRepository> userRepositoryMap= gitHub.getMyself().getAllRepositories();
                        //userRepositoryMap.get(0).queryCommits().list().asList().get(0).getCommitShortInfo().getMessage();
                        model.addAttribute(Constants.gitUserRepositoryDetails, repositoryDetails.getAsList(userRepositoryMap));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "/projectHome";
                }
            }
            else
            {
                return "redirect:/login";
            }
        }

    }
    @RequestMapping("/projectLogIn")
    public String projectLogIn(@RequestParam("Username") String gitUserName, @RequestParam("Password") String gitPassword, HttpSession httpSession, Model model)
    {
        String username=(String)httpSession.getAttribute(Constants.userName);
        if(username==null ||(username.length()==0))
        {
            return "redirect:/login";
        }
        else
        {
            List<Users> users = userRepository.findByUserName(username);
            if(!users.isEmpty()){
                users.get(0).setGitUserId(gitUserName);
                users.get(0).setGitPassword(gitPassword);
                userRepository.save(users.get(0));
                GitHub gitHub= null;
                try {
                    gitHub = GitHub.connectUsingPassword(gitUserName,gitPassword);
                    Map<String, GHRepository> userRepositoryMap=null;
                    for(;true;)
                    {

                        try
                        {
                            userRepositoryMap = gitHub.getMyself().getAllRepositories();
                            break;
                        }
                        catch (SocketTimeoutException exception)
                        {
                            System.out.println("Socket timed out retrying");
                            exception.printStackTrace();
                        }
                    }
                    model.addAttribute(Constants.gitUserRepositoryDetails, repositoryDetails.getAsList(userRepositoryMap));


                } catch (IOException e) {
                    e.printStackTrace();
                }

                return "redirect:/project";
            }
            else
            {
                return "redirect:/login";
            }
        }
    }
    @RequestMapping("/repoTraverse")
    public String repositoryTraverse(@ModelAttribute("selects")String  selects, Model model, HttpSession httpSession, RedirectAttributes redirectAttributes) {
        String name = (String) httpSession.getAttribute(Constants.userName);
        model.addAttribute(Constants.name_in_page, httpSession.getAttribute(Constants.user_name).toString());
        if((selects==null)||(selects.length()==0))
        {
            selects=(String)redirectAttributes.getFlashAttributes().get("selects");
        }
        if (name == null || (name.length() == 0)) {
            return "redirect:/login";
        }
        else {
            List<Users> user = userRepository.findByUserName(name);
            if (user.size() != 0) {
                if ((user.get(0).getGitUserId() == null) || user.get(0).getGitUserId().length() == 0)
                {
                    return "/gitLogin";
                }
                else
                {
                    String gitUserName = (userRepository.findByUserName(name).get(0).getGitUserId());
                    String gitPassword = (userRepository.findByUserName(name).get(0).getGitPassword());
                    String path = (String) httpSession.getAttribute(Constants.currentPath);
                    Stack<List<GHContent>> stateStack = (Stack) httpSession.getAttribute(Constants.holderStack);
                    Stack<String> selectStack=(Stack)httpSession.getAttribute(Constants.selectStack);
                    if (stateStack == null) {
                        stateStack = new Stack<>();
                    }
                    if (selectStack == null) {
                        selectStack = new Stack<>();
                    }
                    selectStack.push(selects);
                    httpSession.setAttribute(Constants.selectStack,selectStack);
//---------------------------------setting path and if first get repo list-----------------------------------------
                    String newPath;
                    if (path == null) {
                        newPath = "/" + selects;
                        httpSession.setAttribute(Constants.currentPath, newPath);


                        GitHub gitHub = null;
                        try {
                            gitHub = GitHub.connectUsingPassword(gitUserName, gitPassword);
                            Map<String, GHRepository> userRepositoryMap = null;
                            for (; true; )// loop is because of retry
                            {

                                try {
                                    userRepositoryMap = gitHub.getMyself().getAllRepositories();
                                    stateStack.push(null);
                                    for(String key:userRepositoryMap.keySet())
                                    {
                                        if(key.compareTo(selects)==0)
                                        {
                                            List<GHContent> folderList =userRepositoryMap.get(key).getDirectoryContent("/");
                                            stateStack.push(folderList);
                                            httpSession.setAttribute(Constants.holderStack,stateStack);
                                            model.addAttribute(Constants.folderList,folderList);
                                            return "/repository";
                                        }
                                    }
                                } catch (SocketTimeoutException exception) {
                                    System.out.println("Socket timed out retrying");
                                    exception.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        newPath = path + "/" + selects;
                        httpSession.removeAttribute(Constants.currentPath);
                        httpSession.setAttribute(Constants.currentPath, newPath);
                        List<GHContent> tempList=stateStack.lastElement();
                        for(GHContent content:tempList)
                        {
                            if(content.getName().compareTo(selects)==0)
                            {
                                if(content.isDirectory())
                                {
                                    try {
                                        List<GHContent> folderList =content.listDirectoryContent().asList();
                                        stateStack.push(folderList);
                                        httpSession.removeAttribute(Constants.holderStack);
                                        httpSession.setAttribute(Constants.holderStack,stateStack);
                                        model.addAttribute(Constants.folderList,folderList);
                                        return  "/repository";
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else
                                {
                                    try {
                                        List<GHContent> temp=stateStack.lastElement();
                                        stateStack.push(temp);
                                        httpSession.removeAttribute(Constants.holderStack);
                                        httpSession.setAttribute(Constants.holderStack,stateStack);
                                        model.addAttribute(Constants.codeData, getStringFromInputStream(content.read()));
                                        String as[] = content.getName().split("\\.");
                                        model.addAttribute(Constants.lastCommit, content.getOwner().listCommits().asList().get(0).getCommitter().getName());
                                        model.addAttribute(Constants.lang, as[as.length - 1]);
                                        model.addAttribute(Constants.fileName, content.getName());
                                        model.addAttribute(Constants.downloadFile, content.getDownloadUrl());
                                        return "/codePage";
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        return "redirect:/login";
    }

    @PostMapping("/goBack")
    public  String onBack(HttpSession httpSession ,Model model,RedirectAttributes redirectAttributes)
    {
        System.out.println("received by go back");
        model.addAttribute(Constants.name_in_page, httpSession.getAttribute(Constants.user_name).toString());
        String userName = (String) httpSession.getAttribute(Constants.userName);
        if (userName == null || (userName.length() == 0)) {
            return "redirect:/login";
        }
        Stack<List<GHContent>> temp= (Stack<List<GHContent>>) httpSession.getAttribute(Constants.holderStack);
        Stack<String> select=(Stack)httpSession.getAttribute(Constants.selectStack);

        String name=select.lastElement();
        select.pop();
        if(select.size()>0) {

            redirectAttributes.addAttribute("selects", select.pop());

        }
        else
        {
            httpSession.removeAttribute(Constants.holderStack);
            httpSession.removeAttribute(Constants.currentPath);
            return "redirect:/project";
        }

        List<GHContent> tempObject=temp.pop();
        if(temp.lastElement()!=null)
        {
            temp.pop();
        }
        /*temp.pop();
        temp.pop();*/
        if((temp.size()<=0)||(temp.lastElement()==null))
        {
            httpSession.removeAttribute(Constants.holderStack);
            httpSession.removeAttribute(Constants.currentPath);
            return "redirect:/repoTraverse";
        }
        else
        {

            String path = (String) httpSession.getAttribute(Constants.currentPath);
            String  [] ar=path.split("/");
            String tempPath="";

            for(int i=1;i<ar.length-2;i++)
            {
                tempPath=tempPath+"/"+ar[i];
            }
            if(tempPath.length()==0)
            {
                tempPath=null;
            }
            httpSession.removeAttribute(Constants.currentPath);
            httpSession.setAttribute(Constants.currentPath,tempPath);
            httpSession.removeAttribute(Constants.holderStack);
            httpSession.setAttribute(Constants.holderStack,temp);


        }
        return "redirect:/repoTraverse";
    }
    @RequestMapping("/addCollaborator")
    public String addCollaborator(@ModelAttribute("Add_in")String  inRepository,@ModelAttribute("Collaborator_name")String collabortor,RedirectAttributes redirectAttributes,HttpSession httpSession)
    {
        String userName = (String) httpSession.getAttribute(Constants.userName);
        if (userName == null || (userName.length() == 0)) {
            return "redirect:/login";
        }
        String gitUserName = (userRepository.findByUserName(userName).get(0).getGitUserId());
        String gitPassword = (userRepository.findByUserName(userName).get(0).getGitPassword());
        GitHub gitHub = null;
        GitHub gitCollaborator=null;
        try {
            gitHub = GitHub.connectUsingPassword(gitUserName, gitPassword);
            GHUser coll=gitHub.getUser(collabortor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, GHRepository> userRepositoryMap=null;
        for (; true; )// loop is because of retry
        {
           try {
               userRepositoryMap = gitHub.getMyself().getAllRepositories();
               for(String key:userRepositoryMap.keySet())
               {
                   if(key.compareTo(inRepository)==0)
                   {
                       GHUser temp=gitHub.getUser(collabortor);
                       userRepositoryMap.get(key).addCollaborators(temp);
                       redirectAttributes.addAttribute("Optional","Available");

                   }
               }
               break;
           } catch (SocketTimeoutException exception) {

           }
           catch (FileNotFoundException fex)
           {
               redirectAttributes.addAttribute("Optional","NotAvailable");
               break;
           }
           catch (IOException e) {
               e.printStackTrace();
               break;
           }

        }

        return "redirect:project";

    }
    @RequestMapping ("/deleteFile")
    public String deleteFile(HttpSession httpSession,Model model,RedirectAttributes redirectAttributes)
    {
        String userName = (String) httpSession.getAttribute(Constants.userName);
        model.addAttribute(Constants.name_in_page, httpSession.getAttribute(Constants.user_name).toString());
        if (userName == null || (userName.length() == 0)) {
            return "redirect:/login";
        }
        Stack<List<GHContent>> temp= (Stack<List<GHContent>>) httpSession.getAttribute(Constants.holderStack);
        Stack<String> select=(Stack)httpSession.getAttribute(Constants.selectStack);

        String name=select.lastElement();
        select.pop();
        redirectAttributes.addAttribute("selects",select.pop());
        for(GHContent content:temp.lastElement())
        {
            if(content.getName().compareTo(name)==0)
            {
                DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
                Date dateobj = new Date();
                try {
                     GHContentUpdateResponse sdf=content.delete("Delete by "+userName+" at "+ df.format(dateobj)+" from 24 school");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        temp.pop();
        temp.pop();
       // temp.pop();
        if((temp.size()<=0)||(temp.lastElement()==null))
        {
            httpSession.removeAttribute(Constants.holderStack);
            httpSession.removeAttribute(Constants.currentPath);
        }
        else
        {

            String path = (String) httpSession.getAttribute(Constants.currentPath);
            String  [] ar=path.split("/");
            String tempPath="";
            for(int i=1;i<ar.length-2;i++)
            {
                tempPath=tempPath+"/"+ar[i];
            }
            httpSession.removeAttribute(Constants.currentPath);
            httpSession.setAttribute(Constants.currentPath,tempPath);


        }
        return "redirect:/repoTraverse";

    }
    public static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {

                sb.append("\n"+line);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }


}
